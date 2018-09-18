package com.lhiot.oc.basic.api;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.leon.microx.support.result.Multiple;
import com.leon.microx.support.result.Tips;
import com.leon.microx.util.ImmutableMap;
import com.leon.microx.util.Jackson;
import com.leon.microx.util.SnowflakeId;
import com.leon.microx.util.StringUtils;
import com.lhiot.oc.basic.domain.BaseOrderInfo;
import com.lhiot.oc.basic.domain.OrderAssortment;
import com.lhiot.oc.basic.domain.OrderProduct;
import com.lhiot.oc.basic.domain.common.PagerResultObject;
import com.lhiot.oc.basic.domain.enums.HdStatus;
import com.lhiot.oc.basic.domain.enums.OrderStatus;
import com.lhiot.oc.basic.domain.enums.PublishExchange;
import com.lhiot.oc.basic.domain.enums.ReceivingWay;
import com.lhiot.oc.basic.domain.inparam.CreateOrderParam;
import com.lhiot.oc.basic.domain.inparam.QueryParam;
import com.lhiot.oc.basic.domain.inparam.ReturnOrderParam;
import com.lhiot.oc.basic.feign.BaseServiceFeign;
import com.lhiot.oc.basic.feign.BaseUserServerFeign;
import com.lhiot.oc.basic.feign.ThirdPartyServiceFeign;
import com.lhiot.oc.basic.feign.domain.Assortment;
import com.lhiot.oc.basic.feign.domain.ProductsStandard;
import com.lhiot.oc.basic.feign.domain.StoreInfo;
import com.lhiot.oc.basic.service.BaseOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@Api("订单基础")
@RequestMapping("/base/orders")
public class BaseOrderApi {

    private final BaseOrderService baseOrderService;
    private final ThirdPartyServiceFeign thirdPartyServiceFeign;
    private final BaseServiceFeign baseServiceFeign;
    private final BaseUserServerFeign baseUserServerFeign;
    private final SnowflakeId snowflakeId;
    private static final String HD_CANCEL_ORDER_SUCCESS_RESULT_STRING = "{\"success\":true}";
    private final RabbitTemplate rabbit;

    private final PaymentService paymentService;

    final private WaitSendOutRefund waitSendOutRefund;
    final private ReceivedRefund receivedRefund;


    @Autowired
    public BaseOrderApi(BaseOrderService baseOrderService,
                        ThirdPartyServiceFeign thirdPartyServiceFeign,
                        BaseServiceFeign baseServiceFeign,
                        BaseUserServerFeign baseUserServerFeign, SnowflakeId snowflakeId,
                        RabbitTemplate rabbit, PaymentService paymentService,
                        WaitSendOutRefund waitSendOutRefund,
                        ReceivedRefund receivedRefund) {
        this.baseOrderService = baseOrderService;
        this.thirdPartyServiceFeign = thirdPartyServiceFeign;
        this.baseServiceFeign = baseServiceFeign;
        this.baseUserServerFeign = baseUserServerFeign;
        this.snowflakeId = snowflakeId;
        this.rabbit = rabbit;
        this.paymentService = paymentService;
        this.waitSendOutRefund = waitSendOutRefund;
        this.receivedRefund = receivedRefund;

    }

    /*** start****************************************海鼎调货处理******************************************************/
    @ApiOperation("海鼎订单调货")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "storeId", value = "调货目标门店", dataType = "Long", required = true),
            @ApiImplicitParam(paramType = "query", name = "operationUser", value = "操作人Id", dataType = "Long", required = true)
    })
    @PutMapping("/{id}/store")
    public ResponseEntity modifyStoreInOrder(@PathVariable Long id, @RequestParam Long storeId, @RequestParam Long operationUser) {
        BaseOrderInfo orderInfo = baseOrderService.findOrderById(id);
        if (!Objects.equals(OrderStatus.WAIT_SEND_OUT, orderInfo.getStatus()) || !Objects.equals(HdStatus.SEND_OUT, orderInfo.getHdStatus())) {
            log.info("订单状态：" + orderInfo.getStatus() + "----海鼎状态：" + orderInfo.getHdStatus());
            return ResponseEntity.ok().body(ImmutableMap.of("code", "400", "msg", "订单状态不可调货！"));
        }
        ResponseEntity<String> hdResponse = thirdPartyServiceFeign.hdOrderCancel(orderInfo.getHdOrderCode(), "海鼎调货");
        if (hdResponse == null || !Objects.equals(HD_CANCEL_ORDER_SUCCESS_RESULT_STRING, hdResponse.getBody())) {
            log.info("海鼎取消订单编号为：" + orderInfo.getHdOrderCode());
            return ResponseEntity.ok().body(ImmutableMap.of("code", "400", "msg", "海鼎取消订单失败，请重试！"));
        }

        //远程查找调货门店
        ResponseEntity<StoreInfo> storeInfoResponseEntity = baseServiceFeign.storeById(storeId);
        if (storeInfoResponseEntity == null || storeInfoResponseEntity.getStatusCodeValue() >= 400) {
            log.info("远程查找调货门店查询失败：" + orderInfo.getHdOrderCode());
            return ResponseEntity.ok().body(ImmutableMap.of("code", "400", "msg", "远程查找调货门店查询失败，请重试！"));
        }
        StoreInfo storeInfo = storeInfoResponseEntity.getBody();


        //海鼎发送失败没有重新机制，且上一个海鼎订单已取消，则需要人工处理
        //若使用队列发海鼎则事务不统一
        //组装发送海鼎数据
        String hdOrderCode = snowflakeId.orderId();//重新生成海鼎编号
        orderInfo.setHdOrderCode(hdOrderCode);

        //设置调货门店
        orderInfo.setStoreId(storeInfo.getStoreId());
        orderInfo.setStoreCode(storeInfo.getStoreCode());
        orderInfo.setStoreName(storeInfo.getStoreName());

        ResponseEntity hdReduceResponse = thirdPartyServiceFeign.hdReduce(orderInfo);
        if (hdReduceResponse == null || hdReduceResponse.getStatusCodeValue() >= 400) {
            return ResponseEntity.ok().body(ImmutableMap.of("code", "400", "msg", "海鼎发送失败！"));
        }
        //修改订单
        baseOrderService.modifyStoreInOrder(orderInfo, operationUser);

        return ResponseEntity.ok().body(ImmutableMap.of("code", "200", "msg", "调货成功！"));
    }

    @ApiOperation("手动发送海鼎")
    @ApiImplicitParam(paramType = "query", name = "ids", value = "订单IDs", dataType = "String", required = true)
    @PutMapping("/pushing/hd")
    public ResponseEntity sendHd(@RequestParam String ids) {
        List<String> idList = Arrays.asList(ids.split(","));
        List<String> failureIdList = new ArrayList<>(); // 记录发送失败的订单ID
        idList.forEach(id -> {
            try {

                BaseOrderInfo orderInfo = baseOrderService.findOrderById(Long.valueOf(id), true, true, true, false, false);

                ResponseEntity responseEntity = thirdPartyServiceFeign.hdReduce(orderInfo);
                if (responseEntity == null || responseEntity.getStatusCodeValue() != 200) {
                    failureIdList.add(id);
                    return;
                }
                BaseOrderInfo updateOrderInfo = new BaseOrderInfo();
                updateOrderInfo.setHdStatus(HdStatus.SEND_OUT);
                updateOrderInfo.setId(Long.valueOf(id));
                //更新订单海鼎状态
                baseOrderService.update(updateOrderInfo);
            } catch (Exception e) {
                log.info("手动发海鼎失败订单：" + id);
                failureIdList.add(id);
            }
        });
        if (CollectionUtils.isEmpty(failureIdList)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.ok().body(ImmutableMap.of("failure", failureIdList));
    }

    /**end****************************************海鼎调货处理******************************************************/


    /**
     * start****************************************基础订单操作接口 wirte by yijun
     **********************************/
    @PostMapping("/create/assortment")
    @ApiOperation("创建订单(套餐)--公共")
    @ApiImplicitParam(paramType = "body", name = "orderParam", dataType = "CreateOrderParam", required = true, value = "创建订单传入参数")
    public ResponseEntity<?> createOrderWithAssortment(@RequestBody CreateOrderParam orderParam) throws Exception {

        Integer couponAmount = orderParam.getCouponAmount();
        Integer payableAmount = orderParam.getAmountPayable();
        //验证参数中优惠金额及商品
        Tips backMsg = baseOrderService.validationParam(orderParam);
        if (backMsg.getCode().equals("-1")) {
            return ResponseEntity.badRequest().body(backMsg.getMessage());
        }

        //判断门店是否存在
        ResponseEntity<StoreInfo> storeInfo = baseServiceFeign.storeByCode(orderParam.getStoreCode());
        if (Objects.isNull(storeInfo) || storeInfo.getStatusCodeValue() >= 400) {
            return ResponseEntity.badRequest().body("查询门店信息失败");
        }
        StoreInfo store = storeInfo.getBody();
        if (Objects.isNull(store)) {
            return ResponseEntity.badRequest().body("查询门店信息不存在");
        }
        String storeCode = store.getStoreCode();
        String storeName = store.getStoreName();
        orderParam.setStoreId(store.getStoreId());
        orderParam.setStoreName(storeName);
        //套餐id构建成以逗号分割的字符串
        List<String> assortmentIdList = orderParam.getAssortments().parallelStream()
                .map(CreateOrderParam.OrderAssortmentParam::getAssortmentId)
                .map(String::valueOf).collect(Collectors.toList());

        String assortmentIds = StringUtils.arrayToDelimitedString(StringUtils.toStringArray(assortmentIdList), ",");
        //基础服务，查询套餐信息
        ResponseEntity<Multiple<Assortment>> assortmentsList = baseServiceFeign.findAssortments(assortmentIds, "yes");

        if (Objects.isNull(assortmentsList) || assortmentsList.getStatusCodeValue() >= 400) {
            return ResponseEntity.badRequest().body("查询套餐信息失败");
        }
        List<Assortment> assortments = assortmentsList.getBody().getArray();
        if (Objects.isNull(assortments) || assortments.isEmpty()) {
            return ResponseEntity.badRequest().body("查询套餐信息不存在");
        }
        //TODO 判断套餐或者套餐商品是否已经下架
        if (baseOrderService.hasInvalidAssortmentOrProducts(assortments)) {
            return ResponseEntity.badRequest().body("套餐商品已经下架");
        }
        //将传入参数中购买份数，设置到基础数据中心，获取的套餐列表中
        //baseOrderService.buyCount(assortments, orderParam);
        //计算订单中套餐的总金额及套餐中商品的总金额
        Map<String, Integer> assortmentAndProductAmount = baseOrderService.assortmentAndProductAmount(orderParam);
        //验证订单金额(此处尚未验证前端传入的优惠金额，因为当前没有优惠券和活动)
        if (!Objects.equals((payableAmount + couponAmount), assortmentAndProductAmount.get("assortmentAmount"))) {
            return ResponseEntity.badRequest().body("订单传入的金额有误");
        }
        //到hd验证库存 包括套餐与商品
        String sku = baseOrderService.checkHdSku(storeName, storeCode, assortments, orderParam.getOrderProducts());
        if (!"SUCCESS".equals(sku)) {
            return ResponseEntity.badRequest().body(sku);
        }
        //写库
        BaseOrderInfo baseOrderInfo = baseOrderService.createOrderWithAssortment(orderParam);
        return ResponseEntity.ok(baseOrderInfo);
    }


    @PostMapping("create/product")
    @ApiOperation("创建订单(商品)--公共")
    @ApiImplicitParam(paramType = "body", name = "orderParam", dataType = "CreateOrderParam", required = true, value = "创建订单传入参数")
    public ResponseEntity<?> createOrderWithProduct(@RequestBody CreateOrderParam orderParam) throws Exception {

        Integer couponAmount = orderParam.getCouponAmount();
        Integer payableAmount = orderParam.getAmountPayable();

        Tips flag = baseOrderService.validationParam(orderParam);
        if (flag.getCode().equals("-1")) {
            return ResponseEntity.badRequest().body(flag);
        }
        //判断门店是否存在
        ResponseEntity<StoreInfo> storeInfo = baseServiceFeign.storeByCode(orderParam.getStoreCode());
        if (Objects.isNull(storeInfo) || storeInfo.getStatusCodeValue() >= 400) {
            return ResponseEntity.badRequest().body("查询门店信息失败");
        }
        StoreInfo store = storeInfo.getBody();
        if (Objects.isNull(store)) {
            return ResponseEntity.badRequest().body("查询门店信息不存在");
        }
        String storeCode = store.getStoreCode();
        String storeName = store.getStoreName();
        orderParam.setStoreId(store.getStoreId());
        orderParam.setStoreName(storeName);

        //计算订单中套餐的总金额及套餐中商品的总金额
        Map<String, Integer> assortmentAndProductAmount = baseOrderService.assortmentAndProductAmount(orderParam);
        if (!Objects.equals((payableAmount + couponAmount), assortmentAndProductAmount.get("productAmount"))) {
            return ResponseEntity.badRequest().body("订单传入的金额有误");
        }
        //到hd验证库存 包括套餐与商品
        String sku = baseOrderService.checkHdSku(storeName, storeCode, null, orderParam.getOrderProducts());
        if (!"SUCCESS".equals(sku)) {
            return ResponseEntity.badRequest().body(sku);
        }
        //组装订单数据，写库
        BaseOrderInfo baseOrderInfo = baseOrderService.createOrderWithProduct(orderParam);

        return ResponseEntity.ok(baseOrderInfo);
    }


    @PostMapping("create/picking")
    @ApiOperation("创建订单(商品)--仓库提货订单")
    @ApiImplicitParam(paramType = "body", name = "orderParam", dataType = "CreateOrderParam", required = true, value = "创建订单传入参数")
    public ResponseEntity<?> createOrderUsePicking(@RequestBody CreateOrderParam orderParam) throws Exception {

        Tips flag = baseOrderService.validationParam(orderParam);
        if (flag.getCode().equals("-1")) {
            return ResponseEntity.badRequest().body(flag);
        }
        //判断门店是否存在
        ResponseEntity<StoreInfo> storeInfo = baseServiceFeign.storeByCode(orderParam.getStoreCode());
        if (Objects.isNull(storeInfo) || storeInfo.getStatusCodeValue() >= 400) {
            return ResponseEntity.badRequest().body("查询门店信息失败");
        }
        StoreInfo store = storeInfo.getBody();
        if (Objects.isNull(store)) {
            return ResponseEntity.badRequest().body("查询门店信息不存在");
        }
        String storeCode = store.getStoreCode();
        String storeName = store.getStoreName();
        orderParam.setStoreId(store.getStoreId());
        orderParam.setStoreName(storeName);

        //到hd验证库存 包括套餐与商品
        String sku = baseOrderService.checkHdSku(storeName, storeCode, null, orderParam.getOrderProducts());
        if (!"SUCCESS".equals(sku)) {
            return ResponseEntity.badRequest().body(sku);
        }
        //组装订单数据，写库
        BaseOrderInfo baseOrderInfo = baseOrderService.createOrderWithProduct(orderParam);
        //TODO 发送扣减仓库扣减商品申请
        //baseUserServerFeign.xxxxx
        if (Objects.equals(ReceivingWay.TO_THE_HOME,orderParam.getReceivingWay())) {
            //TODO 跳转到支付页面去支付配送费 并且写支付日志为订单价值+配送费 方便退款时候统一处理
        }else{
            //TODO 确认扣除仓库库存
            //baseUserServerFeign.crf.....
            //TODO 发送海鼎订单 修改订单状态 写支付日志为订单价值 支付方式为 鲜果币支付 方便退款时候统一处理
        }

        return ResponseEntity.ok(baseOrderInfo);
    }

    @ApiOperation("订单退货-(套餐和商品列表)-公共")
    @ApiImplicitParam(paramType = "body", name = "returnOrderParam", value = "退货传入参数", required = true, dataType = "ReturnOrderParam")
    @PutMapping("/refund/assortment")
    public ResponseEntity<Tips> refundAssortment(@RequestBody ReturnOrderParam returnOrderParam) throws Exception {
        //Long userId = returnOrderParam.getUserId();
        Long orderId = returnOrderParam.getOrderId();
        String assortmentId = returnOrderParam.getAssortmentIds();
        BaseOrderInfo orderInfo = baseOrderService.findOrderById(orderId, true, true, true, false, false);
        if (Objects.isNull(orderInfo)) {
            return ResponseEntity.ok(Tips.of(-1, "要退货的订单不存在！"));
        }

        if (!orderInfo.getUserId().equals(returnOrderParam.getUserId())) {
            return ResponseEntity.ok(Tips.of(-1, "要退货的订单所属用户与请求用户不一致"));
        }
        String stantdardIds = null;
        List<OrderProduct> refundProducts = null;
        if (StringUtils.isNotBlank(assortmentId)) {
            List<Long> idList = Arrays.asList(assortmentId.split(",")).stream()
                    .map(id -> Long.parseLong(id.trim())).collect(Collectors.toList());
            //退货的套餐
            List<OrderAssortment> assortments = baseOrderService.findByOrderIdAndassormentId(orderId, idList);
            if (Objects.isNull(assortments)) {
                return ResponseEntity.ok(Tips.of(-1, "查询的退货套餐不存在！"));
            }
            //设置套餐
            orderInfo.setOrderAssortment(assortments);
            //退货的商品
            refundProducts = baseOrderService.findRefundProduts(idList, orderId);
            if (Objects.isNull(refundProducts)) {
                return ResponseEntity.ok(Tips.of(-1, "查询的退货商品不存在！"));
            }
            //查询退货套餐中的商品的规格id
            List<String> productId = refundProducts.stream().map(OrderProduct::getStandardId)
                    .map(String::valueOf).collect(Collectors.toList());
            //套餐对应的规格id
            stantdardIds = StringUtils.arrayToDelimitedString(StringUtils.toStringArray(productId), ",");
        } else {
            if (StringUtils.isBlank(returnOrderParam.getOrderBarcodeIds())) {
                return ResponseEntity.ok(Tips.of(-1, "传递的退货商品为空！"));
            }
            //直接退商品
            List<Long> idList = Arrays.asList(returnOrderParam.getOrderBarcodeIds().split(",")).stream()
                    .map(id -> Long.parseLong(id.trim())).collect(Collectors.toList());

            refundProducts = baseOrderService.findProductsByOrderIdAndStandardIds(idList, orderId);

            if (idList.size() != refundProducts.size()) {
                return ResponseEntity.ok(Tips.of(-1, "查询的退货商品不存在！"));
            }
            List<String> productId = refundProducts.stream().map(OrderProduct::getStandardId)
                    .map(String::valueOf).collect(Collectors.toList());
            stantdardIds = StringUtils.arrayToDelimitedString(StringUtils.toStringArray(productId), ",");
        }


        ResponseEntity<Multiple<ProductsStandard>> entity = baseServiceFeign.productByStandardIds("ids", stantdardIds);
        if (Objects.isNull(entity) || entity.getStatusCodeValue() >= 400) {
            return ResponseEntity.ok(Tips.of(-1, "远程查询的商品规格错误！"));
        }

        //设置退货订单barcode
        for (OrderProduct orderProduct : refundProducts) {
            for (ProductsStandard product : entity.getBody().getArray()) {
                if (Objects.equals(orderProduct.getStandardId(), product.getId())) {
                    orderProduct.setBarcode(product.getBarcode());
                    break;
                }
            }
        }
        //设置商品
        orderInfo.setOrderProducts(refundProducts);


        //计算退款金额
/*        int partGoodFee = 0;
        List<OrderAssortment> orderAssortments = orderInfo.getOrderAssortment();
        for(OrderAssortment orderAssortment : orderAssortments){
            partGoodFee += orderAssortment.getBuyCount() * orderAssortment.getAmountPayable();
        }*/
        //海鼎退货及退款
        /*String result = fruitDoctorService.hdRefundDetail(orderInfo, returnOrderParam,baseUserId,partGoodFee);
        if(!Objects.equals(result, "0")){
            return ResponseEntity.badRequest().body(result);
        }*/
        return ResponseEntity.ok(paymentService.refundOrder(returnOrderParam));
    }

    @ApiOperation("取消订单-非仓库订单")
    @ApiImplicitParam(paramType = "path", name = "id", value = "订单id", required = true, dataType = "Long")
    @PutMapping("/cancel/{id}")
    public ResponseEntity cancelOrder(@PathVariable Long id) {
        BaseOrderInfo searchBaseOrderInfo = baseOrderService.findOrderById(id, true, true, false, false, false);
        if (Objects.isNull(searchBaseOrderInfo)) {
            return ResponseEntity.badRequest().body(Tips.of(-1, "未找到订单"));
        }
        //构造退货对象信息
        ReturnOrderParam returnOrderParam = new ReturnOrderParam();
        returnOrderParam.setReturnReason("取消订单");

        List<String> orderAssortmentIdList = searchBaseOrderInfo.getOrderAssortment().parallelStream()
                .map(OrderAssortment::getId)
                .map(String::valueOf).collect(Collectors.toList());
        String orderAssortmentIds = StringUtils.arrayToDelimitedString(StringUtils.toStringArray(orderAssortmentIdList), ",");
        returnOrderParam.setAssortmentIds(orderAssortmentIds);

        List<String> barcodeIdList = searchBaseOrderInfo.getOrderProducts().parallelStream()
                .map(OrderProduct::getBarcode)
                .map(String::valueOf).collect(Collectors.toList());
        String barcodeIds = StringUtils.arrayToDelimitedString(StringUtils.toStringArray(barcodeIdList), ",");
        returnOrderParam.setOrderBarcodeIds(barcodeIds);

        List<String> orderStandardIdList = searchBaseOrderInfo.getOrderProducts().parallelStream()
                .map(OrderProduct::getStandardId)
                .map(String::valueOf).collect(Collectors.toList());
        String orderStandardIds = StringUtils.arrayToDelimitedString(StringUtils.toStringArray(orderStandardIdList), ",");
        returnOrderParam.setOrderProductIds(orderStandardIds);
        returnOrderParam.setOrderId(searchBaseOrderInfo.getId());
        returnOrderParam.setUserId(searchBaseOrderInfo.getUserId());
        //退货处理数据库数据
        boolean refundResult=baseOrderService.refundUpdateOrderAndGoods(returnOrderParam, OrderStatus.FAILURE, searchBaseOrderInfo.getStatus(), searchBaseOrderInfo);
        //发布广播消息 add Limiaojun by 20180609
        if(refundResult) {
            try {
                rabbit.convertAndSend(PublishExchange.REFUND_EXCHANGE.getName(), "", Jackson.json(searchBaseOrderInfo));
            } catch (JsonProcessingException e) {
                log.error("转换json出错");
            }
            return ResponseEntity.ok().build();
        }else{
            return ResponseEntity.badRequest().body("取消订单失败");
        }
    }

    @ApiOperation("取消订单-仓库订单")
    @ApiImplicitParam(paramType = "path", name = "id", value = "订单id", required = true, dataType = "Long")
    @PutMapping("/cancel/picking/{id}")
    public ResponseEntity cancelPickingOrder(@PathVariable Long id) {
        BaseOrderInfo searchBaseOrderInfo = baseOrderService.findOrderById(id, true, false, false, false, false);
        if (Objects.isNull(searchBaseOrderInfo)) {
            return ResponseEntity.badRequest().body(Tips.of(-1, "未找到订单"));
        }
        //构造退货对象信息
        ReturnOrderParam returnOrderParam = new ReturnOrderParam();
        returnOrderParam.setReturnReason("取消订单");

        List<String> orderAssortmentIdList = searchBaseOrderInfo.getOrderAssortment().parallelStream()
                .map(OrderAssortment::getId)
                .map(String::valueOf).collect(Collectors.toList());
        String orderAssortmentIds = StringUtils.arrayToDelimitedString(StringUtils.toStringArray(orderAssortmentIdList), ",");
        returnOrderParam.setAssortmentIds(orderAssortmentIds);

        List<String> barcodeIdList = searchBaseOrderInfo.getOrderProducts().parallelStream()
                .map(OrderProduct::getBarcode)
                .map(String::valueOf).collect(Collectors.toList());
        String barcodeIds = StringUtils.arrayToDelimitedString(StringUtils.toStringArray(barcodeIdList), ",");
        returnOrderParam.setOrderBarcodeIds(barcodeIds);

        List<String> orderStandardIdList = searchBaseOrderInfo.getOrderProducts().parallelStream()
                .map(OrderProduct::getStandardId)
                .map(String::valueOf).collect(Collectors.toList());
        String orderStandardIds = StringUtils.arrayToDelimitedString(StringUtils.toStringArray(orderStandardIdList), ",");
        returnOrderParam.setOrderProductIds(orderStandardIds);
        returnOrderParam.setOrderId(searchBaseOrderInfo.getId());
        returnOrderParam.setUserId(searchBaseOrderInfo.getUserId());
        //退货处理数据库数据
        boolean refundResult=baseOrderService.refundUpdateOrderAndGoods(returnOrderParam, OrderStatus.FAILURE, searchBaseOrderInfo.getStatus(), searchBaseOrderInfo);

        if(refundResult){
            //TODO 处理退回仓库商品信息
            //baseUserServiceFeign.........
            //发布广播消息 add Limiaojun by 20180609
            try {
                rabbit.convertAndSend(PublishExchange.REFUND_EXCHANGE.getName(), "", Jackson.json(searchBaseOrderInfo));
            } catch (JsonProcessingException e) {
                log.error("转换json出错");
            }
            return ResponseEntity.ok().build();
        }else{
            return ResponseEntity.badRequest().body("取消订单失败");
        }
    }

    @ApiOperation("订单退货")
    @ApiImplicitParam(paramType = "body", name = "returnOrderParam", value = "退货传入参数", required = true, dataType = "ReturnOrderParam")
    @PutMapping("/refund")
    public ResponseEntity returnProducts(@RequestBody ReturnOrderParam returnOrderParam) throws Exception {

        BaseOrderInfo orderInfo = baseOrderService.findOrderById(returnOrderParam.getOrderId(), true, false, false, false, false);
        if (ObjectUtils.isEmpty(orderInfo)) {
            return ResponseEntity.badRequest().body("要退货的订单不存在！");
        }
        if (!orderInfo.getUserId().equals(returnOrderParam.getUserId())) {
            return ResponseEntity.badRequest().body("查找的订单与用户信息不符！");
        }
        ResponseEntity<Multiple<ProductsStandard>> responseEntity = baseServiceFeign.productByStandardIds("ids", returnOrderParam.getOrderBarcodeIds());
        if (responseEntity == null || responseEntity.getStatusCodeValue() >= 400) {
            return ResponseEntity.badRequest().body("查找的订单与用户信息不符！");
        }
        //过滤无需退货的商品
        List<OrderProduct> refundProduct = new ArrayList<>();
        responseEntity.getBody().getArray().forEach(standardProduct -> orderInfo.getOrderProducts().stream()
                .filter(orderProduct -> Objects.equals(standardProduct.getId(), orderProduct.getStandardId()))
                .forEach(orderProduct -> {
                    orderProduct.setProductName(standardProduct.getProductName());
                    orderProduct.setBarcode(standardProduct.getBarcode());
                    refundProduct.add(orderProduct);
                }));
        orderInfo.setOrderProducts(refundProduct);
        Tips tips;
        // 使用海鼎订单编号 查询海鼎订单状态
        ResponseEntity<Map<String, Object>> hdOrderDetail = thirdPartyServiceFeign.hdOrderDetail(orderInfo.getHdOrderCode());
        if (hdOrderDetail == null || hdOrderDetail.getStatusCodeValue() >= 400) {
            return ResponseEntity.badRequest().body("查询订单失败");
        }
        Map<String, Object> orderMap = hdOrderDetail.getBody();
        log.info(hdOrderDetail.toString());
        // 海鼎已确认
        if (Objects.equals(OrderStatus.WAIT_SEND_OUT, orderInfo.getStatus())
                && !CollectionUtils.isEmpty(orderMap)
                && "confirmed".equals(orderMap.get("state"))
                ) {
            tips = waitSendOutRefund.doRefund(orderInfo, returnOrderParam);
        } else if (OrderStatus.RECEIVED.equals(orderInfo.getStatus())
                && !CollectionUtils.isEmpty(orderMap)
                && ("delivering".equals(orderMap.get("state"))
                || "delivered".equals(orderMap.get("state")))) {
            // 海鼎配送中和配送完成
            tips = receivedRefund.doRefund(orderInfo, returnOrderParam);
        } else {
            return ResponseEntity.badRequest().body("当前状态不能退货！");
        }
        log.debug("退货处理结果," + tips);

        if (!Objects.equals(tips.getCode(), "1")) {
            return ResponseEntity.badRequest().body(tips.getMessage());
        }
        rabbit.convertAndSend(PublishExchange.REFUND_EXCHANGE.getName(), "", Jackson.json(orderInfo));
        return ResponseEntity.ok(tips.getCode());
    }


    @ApiOperation("根据订单id查询订单详情")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "orderId", dataType = "Long", required = true, value = "订单id"),
            @ApiImplicitParam(paramType = "query", name = "needProductList", dataType = "boolean", required = true, value = "是否需要加载商品信息"),
            @ApiImplicitParam(paramType = "query", name = "needAssortmentList", dataType = "boolean", required = true, value = "是否需要加载套餐信息"),
            @ApiImplicitParam(paramType = "query", name = "needAssortmentProductList", dataType = "boolean", required = true, value = "是否需要加载套餐中商品信息"),
            @ApiImplicitParam(paramType = "query", name = "needOrderFlowList", dataType = "boolean", required = true, value = "是否需要加载订单操作流水信息"),
            @ApiImplicitParam(paramType = "query", name = "needDeliverNoteList", dataType = "boolean", required = true, value = "是否需要加载订单配送信息")
    })
    /**
     * @author yj
     * @param orderId
     * @param needProductList 是否需要加载商品信息
     * @param needAssortmentList 是否需要加载套餐信息
     * @param needAssortmentProductList 是否需要加载套餐中商品信息
     * @param needOrderFlowList 是否需要加载订单操作流水信息
     * @param needDeliverNoteList 是否需要加载订单配送信息
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<?> orderDetail(@PathVariable Long orderId, @RequestParam("needProductList") boolean needProductList, @RequestParam("needAssortmentList") boolean needAssortmentList,
                                         @RequestParam("needAssortmentProductList") boolean needAssortmentProductList, @RequestParam("needOrderFlowList") boolean needOrderFlowList,
                                         @RequestParam("needDeliverNoteList") boolean needDeliverNoteList) {
        BaseOrderInfo order = baseOrderService.findOrderById(orderId, needProductList, needAssortmentList, needAssortmentProductList, needOrderFlowList, needDeliverNoteList);
        if (Objects.isNull(order)) {
            return ResponseEntity.badRequest().body(Tips.of(-1, "获取订单失败"));
        }
        return ResponseEntity.ok(order);
    }

    @ApiOperation("查询订单分页列表(前端、后台管理系统通用)")
    @PostMapping("/query")
    @ApiImplicitParam(paramType = "body", name = "queryParam", dataType = "QueryParam", required = true, value = "查询订单列表传入参数")
    public PagerResultObject<BaseOrderInfo> orders(@RequestBody QueryParam queryParam) {
        //默认创建时间倒序
        if (Objects.isNull(queryParam.getSidx())) {
            queryParam.setSidx("create_at");
            queryParam.setSord("desc");
        }
        return baseOrderService.pageList(queryParam);
    }

    @ApiOperation("判断商品是否有货")
    @GetMapping("/checkStock")
    public ResponseEntity prepay(@RequestParam("storeCode") String storeCode, @RequestParam("productIds") String productIds) {
        List<ProductsStandard> ProductsStandards = baseOrderService.findProductStardands(productIds);
        Map<String, Object> result = baseOrderService.checkHdSku(storeCode, ProductsStandards);
        if (null != result) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body("查询失败");
        }
    }
    /************************************************************************************************************/

    @ApiOperation("修改订单为配送中")
    @PutMapping("/transfering/{orderId}")
    public ResponseEntity transfering(@PathVariable("orderId") Long orderId){
        BaseOrderInfo searchBaseOrderInfo = baseOrderService.findOrderById(orderId);
        if(Objects.isNull(searchBaseOrderInfo)){
            return ResponseEntity.badRequest().body("未找到订单");
        }

        BaseOrderInfo baseOrderInfo=new BaseOrderInfo();
        baseOrderInfo.setStatus(OrderStatus.DISPATCHING);
        baseOrderInfo.setId(orderId);
        baseOrderService.updateOrderStatusById(baseOrderInfo);
        return ResponseEntity.ok().build();
    }

    @ApiOperation("修改订单为已收货")
    @PutMapping("/received/{orderId}")
    public ResponseEntity received(@PathVariable("orderId") Long orderId){
        BaseOrderInfo searchBaseOrderInfo = baseOrderService.findOrderById(orderId);
        if(Objects.isNull(searchBaseOrderInfo)){
            return ResponseEntity.badRequest().body("未找到订单");
        }

        BaseOrderInfo baseOrderInfo=new BaseOrderInfo();
        baseOrderInfo.setStatus(OrderStatus.RECEIVED);
        baseOrderInfo.setId(orderId);
        baseOrderService.updateOrderStatusById(baseOrderInfo);
        return ResponseEntity.ok().build();
    }

    @ApiOperation("依据海鼎订单编码查询订单信息")
    @GetMapping("/by-hd-order-code/{hdOrderCode}")
    public ResponseEntity findByHdCode(@PathVariable("hdOrderCode") String hdOrderCode){
       return ResponseEntity.ok(baseOrderService.findByHdCode(hdOrderCode));
    }
}
