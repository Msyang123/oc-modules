package com.lhiot.oc.basic.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.leon.microx.common.wrapper.Multiple;
import com.leon.microx.common.wrapper.Tips;
import com.leon.microx.util.*;
import com.lhiot.oc.basic.domain.*;
import com.lhiot.oc.basic.domain.enums.*;
import com.lhiot.oc.basic.domain.inparam.*;
import com.lhiot.oc.basic.feign.*;
import com.lhiot.oc.basic.feign.domain.*;
import com.lhiot.oc.basic.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class BaseOrderService {

    private final SnowflakeId snowflakeId;
    private final BaseServiceFeign baseServiceFeign;
    private final BaseUserServerFeign baseUserServerFeign;
    private final ThirdPartyServiceFeign thirdPartyServiceFeign;
    private final OrderMapper orderMapper;
    private final OrderAssortmentMapper orderAssortmentMapper;
    private final OrderFlowMapper orderFlowMapper;
    private final OrderProductMapper orderProductMapper;
    private final TransferOrderLogMapper transferOrderLogMapper;
    private final RabbitTemplate rabbit;


    @Autowired
    public BaseOrderService(SnowflakeId snowflakeId,
                            BaseServiceFeign baseServiceFeign,
                            BaseUserServerFeign baseUserServerFeign,
                            ThirdPartyServiceFeign thirdPartyServiceFeign,
                            OrderMapper orderMapper,
                            OrderAssortmentMapper orderAssortmentMapper,
                            OrderFlowMapper orderFlowMapper,
                            OrderProductMapper orderProductMapper,
                            TransferOrderLogMapper transferOrderLogMapper, RabbitTemplate rabbit) {
        this.snowflakeId = snowflakeId;
        this.baseServiceFeign = baseServiceFeign;
        this.baseUserServerFeign = baseUserServerFeign;
        this.thirdPartyServiceFeign = thirdPartyServiceFeign;
        this.orderMapper = orderMapper;
        this.orderAssortmentMapper = orderAssortmentMapper;
        this.orderProductMapper = orderProductMapper;
        this.orderFlowMapper = orderFlowMapper;
        this.transferOrderLogMapper = transferOrderLogMapper;
        this.rabbit = rabbit;
    }

    /**
     * 验证创建订单数据 可以是套餐 也可以是商品
     *
     * @param param 创建订单参数
     * @return
     */
    public Tips validationParam(CreateOrderParam param) {
        //应付金额为空或者小于零
        int amountPayable = param.getAmountPayable();
        if(Objects.isNull(amountPayable) || amountPayable <= 0){
            return  Tips.of(-1,"应付金额为空或者小于零");
        }

        //商品为空
        List<CreateOrderParam.OrderAssortmentParam> assortments = param.getAssortments();
        List<CreateOrderParam.OrderProductParam> orderProducts = param.getOrderProducts();
        if((Objects.isNull(assortments) || assortments.isEmpty())&&(Objects.isNull(orderProducts) || orderProducts.isEmpty())){
            return Tips.of(-1,"商品或者套餐为空");
        }
        //校验套餐数量
        if(Objects.nonNull(assortments)){
            for (CreateOrderParam.OrderAssortmentParam assortment : assortments) {
                //判断传入购买份数
                Integer buyCount = assortment.getBuyCount();
                if (Objects.isNull(buyCount) || buyCount<= 0) {
                    return Tips.of(-1,"套餐购买数量为0");
                }
            }
        }
        //校验商品数量
        if(Objects.nonNull(orderProducts)){
            for (CreateOrderParam.OrderProductParam orderProductParam : orderProducts) {
                //判断传入购买份数
                Integer buyCount = orderProductParam.getBuyCount();
                if (Objects.isNull(buyCount) || buyCount<= 0) {
                    return Tips.of(-1,"商品购买数量为0");
                }
            }
            List<String> standardIdList = orderProducts.parallelStream()
                    .map(CreateOrderParam.OrderProductParam::getStandardId)
                    .map(String::valueOf).collect(Collectors.toList());
            String standardIds = org.springframework.util.StringUtils.arrayToDelimitedString(StringUtils.toStringArray(standardIdList), ",");

            ResponseEntity<Multiple<ProductsStandard>> responseEntity = baseServiceFeign.productByStandardIds("ids", standardIds);
            if (Objects.equals(responseEntity, null) || responseEntity.getStatusCodeValue() >= 400) {
                return Tips.of(-1, "获取商品信息失败");
            }
        }

        Integer couponAmount = Objects.isNull(param.getCouponAmount())?0:param.getCouponAmount();
        if(couponAmount >= param.getTotalAmount()){
            return Tips.of(-1,"优惠金额不能大于订单总金额");
        }

        //送货上门的订单，地址不能为空
        ReceivingWay receivingWay = param.getReceivingWay();
        if (ReceivingWay.TO_THE_HOME.equals(receivingWay)&&Objects.isNull(param.getAddress())) {
            return Tips.of(-1,"送货上门，地址为空");
        }
        return Tips.of(1,"校验成功");
    }

    /**
     * 判断套餐列表中是否存在无效的套餐或者套餐商品
     * @param assortments 创建订单时套餐个数
     * @return
     */
    public boolean hasInvalidAssortmentOrProducts(List<Assortment> assortments){
        boolean invalid = true;
        for(Assortment assortment : assortments){
            invalid = true;
            String assortmentStatus = assortment.getStatus();
            //判断套餐是否无效
            if("INVALID".equals(assortmentStatus)){
                break;
            }
            List<ProductsStandard> products = assortment.getAssortmentProducts();
            if(Objects.isNull(products) || products.isEmpty()){
                break;
            }
            //判断是商品列表中是否存在下架的商品
            for(ProductsStandard ps : products){
                if("INVALID".equals(ps.getShelvesStatus())){
                    break;
                }
            }
            invalid = false;
        }
        return invalid;
    }


    /**
     * 计算订单中套餐总金额及套餐中商品总金额
     * @param orderParam 创建订单参数(包括订单信息 商品信息 套餐信息 如果以套餐购买，需要将套餐拆解成商品信息)
     * @return
     */
    public Map<String,Integer> assortmentAndProductAmount(CreateOrderParam orderParam){
        Integer assortmentAmount = 0;
        Integer productAmount = 0;
        //分别计算套餐价格和商品价格
        if(Objects.nonNull(orderParam.getAssortments())) {
            for (CreateOrderParam.OrderAssortmentParam assortment : orderParam.getAssortments()) {
                assortmentAmount += assortment.getBuyCount() * assortment.getPrice();
            }
        }
        if(Objects.nonNull(orderParam.getOrderProducts())) {
            for (CreateOrderParam.OrderProductParam product : orderParam.getOrderProducts()) {
                productAmount += product.getPrice() * product.getBuyCount();
            }
        }
        Map<String,Integer> resultMap=new HashMap<>(2);
        resultMap.put("assortmentAmount", assortmentAmount);
        resultMap.put("productAmount", productAmount);
        return	resultMap;
    }

    /**
     * 海鼎库存校验
     * @return
     */
    @SuppressWarnings("unchecked")
    public String checkHdSku(String storeName,String storeCode,List<Assortment> assortments,List<CreateOrderParam.OrderProductParam> orderProducts){
        String result = null;
        List<String> skuids = new ArrayList<>();
        List<ProductsStandard> products = new ArrayList<>();

        //套餐
        if(assortments!=null) {
            for (Assortment assortment : assortments) {
                List<ProductsStandard> assortmentProducts = assortment.getAssortmentProducts();
                for (ProductsStandard product : assortmentProducts) {
                    product.setAssortmentName(assortment.getAssortmentName());
                    product.setAssortmentId(assortment.getId());
                    product.setLimitQty(product.getLimitQty());
                    products.add(product);
                    skuids.add(product.getBarcode());
                }
            }
        }

        //商品
        List<String> standardIdList = orderProducts.parallelStream()
                .map(CreateOrderParam.OrderProductParam::getStandardId)
                .map(String::valueOf).collect(Collectors.toList());
        String standardIds = StringUtils.arrayToDelimitedString(StringUtils.toStringArray(standardIdList), ",");

        ResponseEntity<Multiple<ProductsStandard>> productsStandardList= baseServiceFeign.productByStandardIds("ids",standardIds);
        //如果调用失败就不去校验
        if(Objects.isNull(productsStandardList)||productsStandardList.getStatusCodeValue()>=400){
            return "SUCCESS";
        }
        List<ProductsStandard> productsStandardResultList=productsStandardList.getBody().getArray();

        for (ProductsStandard orderProduct:productsStandardResultList){
            //当前列表中是否包含商品
            boolean ifInclude=false;
            for(ProductsStandard productsStandard:products){
                //包含此商品
                if(Objects.equals(orderProduct.getId(),productsStandard.getId())){
                    ifInclude=true;
                    break;
                }
            }
            //不包含就直接加入到列表中
            if(!ifInclude){
                products.add(orderProduct);
                skuids.add(orderProduct.getBarcode());
            }
        }
        String[] skuId = new String[skuids.size()];
        ResponseEntity<Map<String, Object>> entity = thirdPartyServiceFeign.querySku(storeCode,skuids.toArray(skuId));
        if(Objects.isNull(entity) || entity.getStatusCodeValue() >= 400){
            return "查询门店库存失败";
        }
        Map<String,Object> skuMap = entity.getBody();
        List<Map<String,Object>> businvs = (List<Map<String, Object>>) skuMap.get("businvs");
        //noSku表示库存不足的套餐
        Set<String> lackOfStore = new HashSet<>();
        //遍历海鼎库存和表中限购数
        for(ProductsStandard product : products){
            String barCode = product.getBarcode();
            int limit = product.getLimitQty();
            for(Map<String,Object> map : businvs){
                String skuid = (String) map.get("skuId");
                if(barCode.equals(skuid)){
                    double qty = new BigDecimal(map.get("qty")+"").doubleValue();
                    if(qty < limit){
                        lackOfStore.add(product.getAssortmentName());
                    }
                }
            }
        }
        //将set的装车字符串
        List<String> temp = new ArrayList<>(lackOfStore);
        if(temp.isEmpty()){
            result = "SUCCESS";
        }else{
            result = storeName +":"+ StringUtils.arrayToDelimitedString(StringUtils.toStringArray(temp), ",");
        }
        //获取库存不足的套餐字符串
        return result;
    }

    /**
     * 海鼎库存校验
     * @return
     */
    public Map<String,Object> checkHdSku(String storeCode, List<ProductsStandard> productsStandardResultList) {
        List<String> skuids = new ArrayList<>();
        for(ProductsStandard product : productsStandardResultList){
            skuids.add(product.getBarcode());
        }
        Map<String,Object> result = new HashMap<String,Object>();
        String[] skuId = new String[skuids.size()];
        ResponseEntity<Map<String, Object>> entity = thirdPartyServiceFeign.querySku(storeCode,skuids.toArray(skuId));
        if(Objects.isNull(entity) || entity.getStatusCodeValue() >= 400){
            return null;
        }
        List<Long> availableList = new ArrayList<Long>();
        List<Long> notAvailableList = new ArrayList<Long>();
        result.put("availableList",availableList);
        result.put("notAvailableList",notAvailableList);
        Map<String,Object> skuMap = entity.getBody();
        List<Map<String,Object>> businvs = (List<Map<String, Object>>) skuMap.get("businvs");
        for(ProductsStandard product : productsStandardResultList){
            String barCode = null;
            barCode = product.getBarcode();
            int limit = product.getLimitQty();
            for(Map<String,Object> map : businvs){
                String skuid = (String) map.get("skuId");
                if(barCode.equals(skuid)){
                    double qty = new BigDecimal(map.get("qty")+""+"").doubleValue();
                    if(qty < limit){
                        notAvailableList.add(product.getId());
                    }else{
                        availableList.add(product.getId());
                    }
                }
            }
        }
        return result;
    }

    /**
     * 创建订单(套餐)，写库 优惠计算待续
     * @param orderParam 传入的参数
     * @return
     */
    public BaseOrderInfo createOrderWithAssortment(CreateOrderParam orderParam){
        BaseOrderInfo baseOrder = orderParam.toOrderObject();

        Integer couponAmount = orderParam.getCouponAmount();
        Map<String,Integer> assortmentAndProductAmount = this.assortmentAndProductAmount(orderParam);
        //订单套餐总金额及订单套餐的商品总金额
        Integer productAmount = assortmentAndProductAmount.get("productAmount");
        Integer assortmentAmount = assortmentAndProductAmount.get("assortmentAmount");
        //套餐的优惠比例
        BigDecimal preAssortment = this.preProportion(couponAmount, assortmentAmount);
        //相对套餐中的商品总的优惠金额
        int totalCoupon = (productAmount-assortmentAmount) + couponAmount;
        BigDecimal preProduct = this.preProportion(totalCoupon, productAmount);

        Long orderId = snowflakeId.longId();

        for (CreateOrderParam.OrderProductParam productParam : orderParam.getOrderProducts()) {
            OrderProduct product = new OrderProduct();
            BeanUtils.of(product).populate(BeanUtils.of(productParam).toMap());
            product.setOrderId(orderId);
            baseOrder.getOrderProducts().add(product);
        }

        //构建写order库的参数
        String code = snowflakeId.stringId();

        baseOrder.setId(orderId);
        baseOrder.setCode(code);
        baseOrder.setApplicationType(ApplicationType.FRUIT_DOCTOR);
        baseOrder.setCreateAt(new Timestamp(System.currentTimeMillis()));
        baseOrder.setHdOrderCode(code);//初始海鼎订单编码为订单编码
        baseOrder.setHdStatus(HdStatus.NOT_SEND);
        baseOrder.setStatus(OrderStatus.WAIT_PAYMENT);
        StoreInfo storeInfo = this.findStore(orderParam.getStoreId());
        baseOrder.setStoreCode(storeInfo.getStoreCode());
        baseOrder.setNickname(orderParam.getNickname());

        //构建写order_assortment库的数据
        //订单的套餐
        List<OrderAssortment> orderAssortments = new ArrayList<>();
        //订单的套餐商品
        //List<OrderProduct> orderProducts = new ArrayList<>();
        //构建写订单套餐表的参数
        //套餐id构建成以逗号分割的字符串
        List<String> assortmentIdList = orderParam.getAssortments().parallelStream()
                .map(CreateOrderParam.OrderAssortmentParam:: getAssortmentId)
                .map(String::valueOf).collect(Collectors.toList());

        String assortmentIds = StringUtils.arrayToDelimitedString(StringUtils.toStringArray(assortmentIdList), ",");
        //基础服务，查询套餐信息
        ResponseEntity<Multiple<Assortment>> assortmentsList = baseServiceFeign.findAssortments(assortmentIds, "yes");
        for(CreateOrderParam.OrderAssortmentParam orderAssortmentParam : orderParam.getAssortments()){
            //订单套餐对象
            OrderAssortment orderAssortment = new OrderAssortment();
            Integer originalPrice = orderAssortmentParam.getPrice();
            Integer buyCount = orderAssortmentParam.getBuyCount();

            orderAssortment.setAssortmentId(orderAssortmentParam.getAssortmentId());

            orderAssortment.setBuyCount(buyCount);
            orderAssortment.setOrderId(orderId);
            orderAssortment.setPrice(originalPrice);//套餐本身的价格
            orderAssortment.setRefundStatus(RefundStatus.NOT_REFUND);

            orderAssortments.add(orderAssortment);
            //依据订单套餐信息远程查询套餐信息
            if(Objects.nonNull(assortmentsList)&&assortmentsList.getStatusCodeValue()<400){
                Assortment assortment=null;
                for(Assortment item: assortmentsList.getBody().getArray()){
                    if(Objects.equals(orderAssortmentParam.getAssortmentId(),item.getId())){
                        assortment=item;
                        break;
                    }
                }
                //查询的套餐信息
                if (Objects.nonNull(assortment)){
                    Integer amountPayable = this.caluCouponFee(originalPrice, preAssortment);

                    orderAssortment.setAmountPayable(amountPayable);//实际支付金额
                    orderAssortment.setAssortmentDiscountPrice(originalPrice-amountPayable);
                    orderAssortment.setAssortmentImage(assortment.getAssortmentImage());
                    orderAssortment.setAssortmentName(assortment.getAssortmentName());



                    List<ProductsStandard> assortmentProducts = assortment.getAssortmentProducts();
                    for(ProductsStandard product : assortmentProducts){
                        //套餐商品
                        Integer productPrice = product.getPrice();
                        Integer realPrice = this.caluCouponFee(productPrice, preProduct);
                        //构建订单商品参数
                        OrderProduct orderProduct = new OrderProduct();
                        orderProduct.setOrderId(orderId);
                        orderProduct.setStandardId(product.getId());
                        orderProduct.setPrice(realPrice);
                        orderProduct.setStandardPrice(productPrice);
                        orderProduct.setProductQty(product.getRelationCount()*buyCount);
                        orderProduct.setStandardQty(product.getStandardQty());
                        orderProduct.setRefundStatus(RefundStatus.NOT_REFUND);
                        orderProduct.setDiscountPrice(realPrice);
                        orderProduct.setAssortmentId(assortment.getId());
                        orderProduct.setProductName(assortment.getAssortmentName()+"_"+product.getProductName());//套餐名+商品名
                        orderProduct.setImage(product.getImage());
                        orderProduct.setSmallImage(product.getSmallImage());
                        orderProduct.setLargeImage(product.getLargeImage());

                        baseOrder.getOrderProducts().add(orderProduct);
                    }
                }
            }
        }
        //写库操作
        orderMapper.create(baseOrder);
        //写套餐信息
        orderAssortmentMapper.createInbatch(orderAssortments);
        //写套餐商品信息
        orderProductMapper.createInBatch(baseOrder.getOrderProducts());
        //构建写order_flow库的数据
        OrderFlow orderFlow = new OrderFlow();
        orderFlow.setOrderId(orderId);
        orderFlow.setStatus(OrderStatus.WAIT_PAYMENT.toString());
        orderFlow.setPreStatus("-");
        orderFlow.setCreateAt(new Timestamp(System.currentTimeMillis()));
        orderFlowMapper.create(orderFlow);

        //发送延时队列
        this.sendCreateMqMessage(baseOrder);

        return baseOrder;
    }

    /**
     * 创建订单(商品)，写库 优惠计算待续
     * @param orderParam 传入的参数
     * @return
     */
    public BaseOrderInfo createOrderWithProduct(CreateOrderParam orderParam){
        BaseOrderInfo baseOrder = orderParam.toOrderObject();

        Integer couponAmount = orderParam.getCouponAmount();
        Map<String,Integer> assortmentAndProductAmount = this.assortmentAndProductAmount(orderParam);
        //订单总金额及订单的商品总金额
        Integer productAmount = assortmentAndProductAmount.get("productAmount");


        //相对套餐中的商品总的优惠金额
        int totalCoupon = productAmount + couponAmount;
        BigDecimal preProduct = this.preProportion(totalCoupon, productAmount);

        Long orderId = snowflakeId.longId();

        //构建写order库的参数
        String code = snowflakeId.stringId();

        baseOrder.setId(orderId);
        baseOrder.setCode(code);
        baseOrder.setApplicationType(ApplicationType.FRUIT_DOCTOR);
        baseOrder.setCreateAt(new Timestamp(System.currentTimeMillis()));
        baseOrder.setHdOrderCode(code);
        baseOrder.setHdStatus(HdStatus.NOT_SEND);
        baseOrder.setStatus(OrderStatus.WAIT_PAYMENT);
        StoreInfo storeInfo = this.findStore(orderParam.getStoreId());
        baseOrder.setStoreCode(storeInfo.getStoreCode());
        baseOrder.setNickname(orderParam.getNickname());

        //查询基础服务中的商品规格
        List<String> standardIdList = orderParam.getOrderProducts().parallelStream()
                .map(CreateOrderParam.OrderProductParam::getStandardId)
                .map(String::valueOf).collect(Collectors.toList());
        String standardIds = org.springframework.util.StringUtils.arrayToDelimitedString(StringUtils.toStringArray(standardIdList), ",");

        ResponseEntity<Multiple<ProductsStandard>> responseEntity = baseServiceFeign.productByStandardIds("ids", standardIds);

        //订单商品
        for(CreateOrderParam.OrderProductParam product : orderParam.getOrderProducts()){
            for(ProductsStandard productsStandard:responseEntity.getBody().getArray()){
                if(Objects.equals(product.getStandardId(),productsStandard.getId())){
                    Integer productPrice = product.getPrice();
                    Integer realPrice = this.caluCouponFee(productPrice, preProduct);
                    //构建订单商品参数
                    OrderProduct orderProduct = new OrderProduct();
                    orderProduct.setOrderId(orderId);
                    orderProduct.setStandardId(product.getStandardId());
                    orderProduct.setPrice(realPrice);
                    orderProduct.setStandardPrice(productPrice);
                    orderProduct.setProductQty(product.getBuyCount());
                    orderProduct.setStandardQty(productsStandard.getStandardQty());
                    orderProduct.setRefundStatus(RefundStatus.NOT_REFUND);
                    orderProduct.setDiscountPrice(realPrice);
                    //orderProduct.setAssortmentId(assortment.getId());非套餐商品
                    orderProduct.setProductName(productsStandard.getProductName());//商品名
                    orderProduct.setImage(productsStandard.getImage());
                    orderProduct.setSmallImage(productsStandard.getSmallImage());
                    orderProduct.setLargeImage(productsStandard.getLargeImage());

                    baseOrder.getOrderProducts().add(orderProduct);
                    break;
                }
            }
        }

        //写库操作
        orderMapper.create(baseOrder);
        //不需要写套餐信息
        //写商品信息
        orderProductMapper.createInBatch(baseOrder.getOrderProducts());
        //构建写order_flow库的数据
        OrderFlow orderFlow = new OrderFlow();
        orderFlow.setOrderId(orderId);
        orderFlow.setStatus(OrderStatus.WAIT_PAYMENT.toString());
        orderFlow.setPreStatus("-");
        orderFlow.setCreateAt(new Timestamp(System.currentTimeMillis()));
        orderFlowMapper.create(orderFlow);

        //发送延时队列
        this.sendCreateMqMessage(baseOrder);

        return baseOrder;
    }
    private void sendCreateMqMessage(BaseOrderInfo baseOrder){
        //发送延时队列
        try {
            //设置三十分钟失效
            rabbit.convertAndSend(DelayExchange.ORDER_DELAY.getExchangeName(), DelayExchange.DelayQueues.ORDER_TIMEOUT.getDelayName(),
                    Jackson.json(baseOrder), message -> {
                        message.getMessageProperties().setExpiration(String.valueOf(30 * 60 * 1000));
                        return message;
                    });
            //订单5天确认收货
            rabbit.convertAndSend(DelayExchange.ORDER_REFUND_DELAY.getExchangeName(), DelayExchange.DelayQueues.ORDER_FINISHED_TIMEOUT.getDelayName(),
                    Jackson.json(baseOrder), message -> {
                        message.getMessageProperties().setExpiration(String.valueOf(5 * 24 * 60 * 60 * 1000));
                        return message;
                    });
            //公共发广播
            rabbit.convertAndSend(PublishExchange.CREATE_EXCHANGE.getName(), "", Jackson.json(baseOrder));
        } catch (JsonProcessingException e) {
            log.error("创建订单发送队列转换错误",e);
        }
    }

    /**
     * 计算优惠比例
     * @param couponAmount 优惠金额
     * @param totalAmount 总金额
     * @return
     * @author yj
     */
    private BigDecimal preProportion(int couponAmount, int totalAmount){
        //优惠金额
        BigDecimal coupon = new BigDecimal(couponAmount);
        //总金额减配送费
        BigDecimal divisor = new BigDecimal(totalAmount);

        BigDecimal proportion = coupon.divide(divisor, 10, BigDecimal.ROUND_UP);
        return new BigDecimal(1).subtract(proportion);
    }

    /**
     * 计算originalPrice优惠后的金额
     * @param originalPrice 原价
     * @param proportion 优惠比例
     * @return
     * @author yj
     */
    private int caluCouponFee(int originalPrice,BigDecimal proportion){
        return new BigDecimal(originalPrice).multiply(proportion).intValue();
    }

    /**
     * 添加订单状态流转信息
     *
     * @param
     * @author yj
     */
    private int createOrderFlow(Long orderId, String orderStatus, String preStatus) {
        OrderFlow orderFlow = new OrderFlow();
        orderFlow.setOrderId(orderId);
        orderFlow.setStatus(orderStatus);
        orderFlow.setPreStatus(preStatus);
        orderFlow.setCreateAt(new Timestamp(System.currentTimeMillis()));
        return orderFlowMapper.create(orderFlow);
    }

    /**
     * 更新订单信息
     * @param baseOrderInfo
     *
     * @author yj
     */
    public void update(BaseOrderInfo baseOrderInfo){
        BaseOrderInfo findBaseOrderInfo=orderMapper.findById(baseOrderInfo.getId());
        if(Objects.isNull(findBaseOrderInfo))
            return;

        int result=orderMapper.update(baseOrderInfo);
        if(result>0&&Objects.nonNull(baseOrderInfo.getStatus())){
            //更新订单状态了 记录流水
            this.createOrderFlow(baseOrderInfo.getId(),baseOrderInfo.getStatus().toString(),findBaseOrderInfo.getStatus().toString());
        }
    }

    /**
     * 依据订单id更新订单状态
     * @param baseOrderInfo
     * @author yj
     */
    public void updateOrderStatusById(BaseOrderInfo baseOrderInfo){
        BaseOrderInfo findBaseOrderInfo=orderMapper.findById(baseOrderInfo.getId());
        if(Objects.isNull(findBaseOrderInfo))
            return;
        int result= orderMapper.updateOrderStatusById(baseOrderInfo);
        if(result>0){
            //更新订单状态了 记录流水
            this.createOrderFlow(baseOrderInfo.getId(),baseOrderInfo.getStatus().toString(),findBaseOrderInfo.getStatus().toString());
        }
    }

    /**
     * 依据订单code更新订单状态
     * @param baseOrderInfo
     * @author yj
     */
    public void updateOrderStatusByCode(BaseOrderInfo baseOrderInfo){
        BaseOrderInfo findBaseOrderInfo=orderMapper.findByCode(baseOrderInfo.getCode());
        if(Objects.isNull(findBaseOrderInfo))
            return;
        int result= orderMapper.updateOrderStatusByCode(baseOrderInfo);
        if(result>0){
            //更新订单状态了 记录流水
            this.createOrderFlow(baseOrderInfo.getId(),baseOrderInfo.getStatus().toString(),findBaseOrderInfo.getStatus().toString());
        }
    }

    /**
     * 订单调货
     * @param baseOrderInfo
     * @param operationUser 操作人
     */
    public void modifyStoreInOrder(BaseOrderInfo baseOrderInfo,Long operationUser){
        //修改订单
        orderMapper.update(baseOrderInfo);
        //添加调货日志
        TransferOrderLog createTransferOrderLog  = new TransferOrderLog();
        createTransferOrderLog.setOrderId(baseOrderInfo.getId());
        createTransferOrderLog.setOrderCode(baseOrderInfo.getHdOrderCode());
        createTransferOrderLog.setOperationUser(operationUser);
        createTransferOrderLog.setStoreId(baseOrderInfo.getStoreId());
        transferOrderLogMapper.create(createTransferOrderLog);
    }


    /**
     * 根据订单ID查询订单信息
     * @author write by yj
     * @param orderId
     * @return
     */
    public BaseOrderInfo findOrderById(Long orderId){
        return this.findOrderById(orderId,false,false,false,false,false);
    }
    /**
     * 根据订单ID查询订单信息
     * @author write by yj
     * @param orderId
     * @param needProductList 需要加载商品信息
     * @param needAssortmentList 需要加载套餐信息
     * @param needAssortmentProductList 需要加载套餐中商品信息
     * @param needOrderFlowList 需要加载订单操作流水信息
     * @param needDeliverNoteList 需要加载订单配送信息
     * @return
     */
    public BaseOrderInfo findOrderById(Long orderId,boolean needProductList,boolean needAssortmentList,boolean needAssortmentProductList,boolean needOrderFlowList,boolean needDeliverNoteList) {
        BaseOrderInfo baseOrderInfo=orderMapper.findById(orderId);
        if(Objects.isNull(baseOrderInfo)){
            return baseOrderInfo;
        }
        //需要加载商品信息
        if(needProductList){
            baseOrderInfo.setOrderProducts(orderProductMapper.findOrderProductsByOrderId(baseOrderInfo.getId()));
            baseOrderInfo.setProductCount(Objects.isNull(baseOrderInfo.getOrderProducts())?0:baseOrderInfo.getOrderProducts().size());
        }
        //需要加载套餐信息
        if(needAssortmentList){
            baseOrderInfo.setOrderAssortment(orderAssortmentMapper.findByOrderId(baseOrderInfo.getId()));
            //需要加载套餐中商品信息
            if(needAssortmentProductList){
                for(OrderAssortment orderAssortment: baseOrderInfo.getOrderAssortment()){
                    orderAssortment.setOrderProductList(orderProductMapper.findProductByAssortmentId(orderAssortment));
                }
            }
        }
        //需要加载订单操作流水信息
        if(needOrderFlowList){
            baseOrderInfo.setOrderFlows(orderFlowMapper.flowByOrderId(baseOrderInfo.getId()));
        }
        //需要加载订单配送信息
        if(needDeliverNoteList){
            baseOrderInfo.setDeliverNoteList(deliverNoteMapper.selectByOrderId(baseOrderInfo.getId()));
        }
        //订单的支付信息
        PaymentLog paymentLog = paymentLogService.getPaymentLog(baseOrderInfo.getId());
        baseOrderInfo.setPaymentLog(paymentLog);
        return baseOrderInfo;
    }

    /**
     * 根据订单code查询订单信息
     * @author write by yj
     * @param code
     * @return
     */
    public BaseOrderInfo findOrderByCode(String code){
        return this.findOrderByCode(code,false,false,false,false,false);
    }
    /**
     * 根据订单code查询订单信息
     * @author write by yj
     * @param code
     * @param needProductList 需要加载商品信息
     * @param needAssortmentList 需要加载套餐信息
     * @param needAssortmentProductList 需要加载套餐中商品信息
     * @param needOrderFlowList 需要加载订单操作流水信息
     * @param needDeliverNoteList 需要加载订单配送信息
     * @return
     */
    public BaseOrderInfo findOrderByCode(String code,boolean needProductList,boolean needAssortmentList,boolean needAssortmentProductList,boolean needOrderFlowList,boolean needDeliverNoteList) {
        BaseOrderInfo baseOrderInfo=orderMapper.findByCode(code);
        if(Objects.isNull(baseOrderInfo)){
            return baseOrderInfo;
        }
        return this.findOrderById(baseOrderInfo.getId(),needProductList, needAssortmentList, needAssortmentProductList, needOrderFlowList, needDeliverNoteList);
    }

    /**
     * 根据海鼎订单code查询订单信息
     * @author write by yj
     * @param hdCode
     * @return
     */
    public BaseOrderInfo findByHdCode(String hdCode){
        return this.findByHdCode(hdCode,false,false,false,false,false);
    }
    /**
     * 根据海鼎订单code查询订单信息
     * @author write by yj
     * @param hdCode
     * @param needProductList 需要加载商品信息
     * @param needAssortmentList 需要加载套餐信息
     * @param needAssortmentProductList 需要加载套餐中商品信息
     * @param needOrderFlowList 需要加载订单操作流水信息
     * @param needDeliverNoteList 需要加载订单配送信息
     * @return
     */
    public BaseOrderInfo findByHdCode(String hdCode,boolean needProductList,boolean needAssortmentList,boolean needAssortmentProductList,boolean needOrderFlowList,boolean needDeliverNoteList) {
        BaseOrderInfo baseOrderInfo=orderMapper.findByHdCode(hdCode);
        if(Objects.isNull(baseOrderInfo)){
            return baseOrderInfo;
        }
        return this.findOrderById(baseOrderInfo.getId(),needProductList, needAssortmentList, needAssortmentProductList, needOrderFlowList, needDeliverNoteList);
    }

    /**
     * Description: 查询订单信息分页列表 rows不传递查询所有
     * @author yj
     * @param queryParam
     * @return
     * @date 2018/08/06 09:10:22
     */
    public PagerResultObject<BaseOrderInfo> pageList(QueryParam queryParam) {
        if(StringUtils.isNotBlank(queryParam.getUserIds())){
            queryParam.setUserIdList(queryParam.getUserIds().split(","));
        }
        long total = 0;
        if (queryParam.getRows() != null && queryParam.getRows() > 0) {
            total = this.count(queryParam);
        }
        return PagerResultObject.of(queryParam, total,
                this.orderMapper.orderQuery(queryParam));
    }

    /**
     * Description: 查询订单信息总记录数
     *
     * @param queryParam
     * @return
     * @author yj
     * @date 2018/08/06 09:10:22
     */
    public long count(QueryParam queryParam){
        return this.orderMapper.orderQueryCounts(queryParam);
    }

    /**
     * 根据订单ID查询退货商品信息
     *
     * @param orderId
     * @return
     */
    public List<OrderProduct> findRefundProductsByOrderId(Long orderId) {
        return orderProductMapper.findRefundProductsByOrderId(orderId);
    }

    /**
     * 根据订单ID查询商品信息
     * @param orderId
     * @return
     */
    public List<OrderProduct> findOrderProductsByOrderId(Long orderId) {
        return orderProductMapper.findOrderProductsByOrderId(orderId);
    }


    //修改订单套餐状态
    public boolean updateOrderAssortment(Long orderId,String refundStatus,List<Long> assortments){
        Map<String,Object> param = ImmutableMap.of("orderId", orderId,
        		"refundStatus", refundStatus,
        		"assortmentIds", assortments);
        return orderAssortmentMapper.updateByOrderIdAndAssortmentId(param) > 0;
    }

    //修改订单商品状态
    public boolean updateOrderProduct(Long orderId,String refundStatus,List<Long> standardIds){
        Map<String,Object> param = ImmutableMap.of("orderId", orderId,
        		"refundStatus", refundStatus,
        		"standardIds", standardIds);
        return orderProductMapper.updateProductByStandardId(param) > 0;
    }



    /**
     * 海鼎取消订单退钱修改订单，订单商品状态，添加订单状态流水
     * @param param
     * @param orderStatus
     * @param preOrderStauts
     * @return
     */
    public boolean refundUpdateOrderAndGoods(ReturnOrderParam param,
    		OrderStatus orderStatus,OrderStatus preOrderStauts,BaseOrderInfo baseOrderInfo) {
    	//修改套餐商品状态
        List<Long> productIdList = Arrays.asList(param.getOrderProductIds().split(","))
                .stream().map(id -> Long.parseLong(id)).collect(Collectors.toList());
    	if(Objects.nonNull(baseOrderInfo) && Objects.nonNull(baseOrderInfo.getOrderAssortment())){
            String assortmentIds = param.getAssortmentIds();
            List<Long> assortmentIdList = Arrays.asList(assortmentIds.split(","))
            		.stream().map(id -> Long.parseLong(id)).collect(Collectors.toList());
            //修改套餐商品及套餐的退款状态
            this.updateOrderAssortment(param.getOrderId(), RefundStatus.REFUND.toString(), assortmentIdList);

    	}
    	//修改订单商品退款状态
        this.updateOrderProduct(param.getOrderId(), RefundStatus.REFUND.toString(), productIdList);
    	//修改订单状态
        BaseOrderInfo orderInfo = new BaseOrderInfo();
        orderInfo.setId(param.getOrderId());
        orderInfo.setReason(param.getReturnReason());
        orderInfo.setStatus(orderStatus);
        int count =orderMapper.update(orderInfo);
        if (count == 1) {
            //创建订单流程
            this.createOrderFlow(param.getOrderId(),orderStatus.toString(),preOrderStauts.toString());
            return true;
        }
        return false;
    }
    /**
     * 鲜果币支付订单
     * @param baseOrderInfo
     * @param baseUserId
     * @param memo
     * @return
     */
    public Tips currencyPayOrder(BaseOrderInfo baseOrderInfo, Long baseUserId, String memo, ApplicationType applicationType){
        //修改订单状态为支付中 不需要支付中状态了 因为没有混合支付
        //baseOrderInfo.setStatus(OrderStatus.PAYMENTING);
        //this.updateOrderStatusByCode(baseOrderInfo);
        //订单操作记录
        //this.createOrderFlow(baseOrderInfo.getId(), OrderStatus.PAYMENTING.toString(),OrderStatus.WAIT_PAYMENT.toString());
        //查询订单支付金额 (应付金额+配送费)
        Integer deliveryAmount = baseOrderInfo.getDeliveryAmount();
        deliveryAmount = Objects.isNull(deliveryAmount)?0:deliveryAmount;
        int needPay=baseOrderInfo.getAmountPayable()+deliveryAmount;

        BaseUser baseUser=new BaseUser();
        baseUser.setCurrency(-needPay);
        baseUser.setId(baseUserId);
        baseUser.setApplicationType(applicationType);

        //扣除鲜果币
        ResponseEntity<Object> entity = baseUserServerFeign.updateCurrencyById(baseUser.getId(), memo, baseUser);
        if(Objects.isNull(entity) || entity.getStatusCodeValue() >= 400) {
            return Tips.of("-1","调用扣减鲜果币失败");
        }else{
            //修改订单状态待收货
            baseOrderInfo.setStatus(OrderStatus.WAIT_SEND_OUT);
            this.updateOrderStatusByCode(baseOrderInfo);
            //this.createOrderFlow(baseOrderInfo.getId(),baseOrderInfo.getOrderStatus().toString(),OrderStatus.PAYMENTING.toString());
            //写支付日志
            PaymentLog paymentLog = new PaymentLog();
            paymentLog.setPayStep("paid");//支付步骤：sign-签名成功 paid-支付成功
            paymentLog.setUserId(baseUserId);//余额支付使用的是基础用户编号 用于退款的时候能够直接依据基础用户信息退鲜果币
            paymentLog.setApplicationType(baseOrderInfo.getApplicationType());
            paymentLog.setSourceType(Attach.SourceType.ORDER.toString());
            paymentLog.setPayType(SignParam.PayPlatformType.BALANCE.toString());
            paymentLog.setPayFee(needPay);//支付金额
            paymentLog.setOrderId(baseOrderInfo.getId());
            paymentLog.setSignAt(new Timestamp(System.currentTimeMillis()));
            paymentLog.setOrderCode(baseOrderInfo.getCode());
            paymentLogService.insertPaymentLog(paymentLog);

            return Tips.of("0","鲜果币支付订单成功");
        }
    }





    /**
     * 依据订单号与规格列表查询订单商品
     * @param standardIds
     * @param orderId
     * @return
     */
    public List<OrderProduct> findProductsByOrderIdAndStandardIds(List<Long> standardIds,Long orderId){
        Map<String,Object> map = ImmutableMap.of("orderId", orderId, "standardIds", standardIds);
        return orderProductMapper.findProductsByOrderIdAndStandId(map);
    }

    /**
     * 发送海鼎
     * @param orderInfo
     * @return
     */
    public boolean sendToHdReduce(BaseOrderInfo orderInfo){
    	log.info("===============>发送海鼎：BaseOrderInfo:"+orderInfo);
    	boolean success = true;
    	ResponseEntity<?> entity = thirdPartyServiceFeign.hdReduce(orderInfo);
    	if(Objects.isNull(entity) || entity.getStatusCodeValue() >= 400){
    		success = false;
    	}
    	return success;
    }



    public List<ProductsStandard> findProductStardands(String ids) {
        return baseServiceFeign.productByStandardIds("ids",ids).getBody().getArray();
    }

    /**
     * 查询退货套餐中商品规格id
     * @param assortmentIds
     * @return
     */
    public List<OrderProduct> findRefundProduts(List<Long> assortmentIds,Long orderId){
        if(Objects.isNull(assortmentIds)){
            return null;
        }
        Map<String,Object> map = ImmutableMap.of("orderId", orderId, "assortmentIds", assortmentIds);
        List<OrderProduct> products = orderProductMapper.findProductByAssortmentIds(map);
        if(Objects.isNull(products) || products.isEmpty()){
            return null;
        }
        return products;
    }




    //获取订单中要退货的套餐
    public List<OrderAssortment> findByOrderIdAndassormentId(Long orderId,List<Long> assortmentIds){
        Map<String,Object> map = ImmutableMap.of("orderId", orderId, "assortmentIds", assortmentIds);
        return orderAssortmentMapper.findByOrderIdAndassormentId(map);
    }

    public StoreInfo findStore(Long id) {
        ResponseEntity<StoreInfo> entity = baseServiceFeign.storeById(id);
        if(Objects.isNull(entity) || entity.getStatusCodeValue() >= 400){
            return null;
        }
        return entity.getBody();
    }

    public StoreInfo findStoreByCode(String storeCode){
        ResponseEntity<StoreInfo> entity = baseServiceFeign.storeByCode(storeCode);
        if(Objects.isNull(entity) || entity.getStatusCodeValue() >= 400){
            return null;
        }
        return entity.getBody();
    }
}
