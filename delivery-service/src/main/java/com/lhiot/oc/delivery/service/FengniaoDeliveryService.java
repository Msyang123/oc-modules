package com.lhiot.oc.delivery.service;

import com.leon.microx.util.Calculator;
import com.leon.microx.util.Jackson;
import com.leon.microx.web.result.Tips;
import com.lhiot.oc.delivery.domain.DeliverBaseOrder;
import com.lhiot.oc.delivery.domain.DeliverNote;
import com.lhiot.oc.delivery.domain.DeliverOrderProduct;
import com.lhiot.oc.delivery.domain.enums.CoordinateSystem;
import com.lhiot.oc.delivery.domain.enums.DeliverType;
import com.lhiot.oc.delivery.domain.enums.DeliveryStatus;
import com.lhiot.oc.delivery.feign.BasicDataService;
import com.lhiot.oc.delivery.feign.domain.Store;
import com.lhiot.oc.delivery.fengniao.FengNiaoDeliveryClient;
import com.lhiot.oc.delivery.fengniao.model.*;
import com.lhiot.oc.delivery.util.Distance;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Transactional
public class FengniaoDeliveryService implements IDelivery {

    private final DeliveryNoteService deliveryNoteService;
    private final BasicDataService basicDataService;
    private final DeliverBaseOrderService deliverBaseOrderService;
    private final FengNiaoDeliveryClient fengNiaoClient;
    private final RedissonClient redissonClient;


    public FengniaoDeliveryService(DeliveryNoteService deliveryNoteService, BasicDataService basicDataService,
                                   DeliverBaseOrderService deliverBaseOrderService,
                                   FengNiaoDeliveryClient fengNiaoClient, RedissonClient redissonClient) {
        this.basicDataService = basicDataService;
        this.fengNiaoClient = fengNiaoClient;
        this.deliveryNoteService = deliveryNoteService;
        this.deliverBaseOrderService = deliverBaseOrderService;
        this.redissonClient = redissonClient;
    }

    /**
     * 发送订单给蜂鸟
     *
     * @return Tips
     */
    public Tips send(CoordinateSystem coordinateSystem, DeliverBaseOrder deliverBaseOrder,BigDecimal distance) {
        log.info("发送蜂鸟的订单{}", deliverBaseOrder);
        ResponseEntity storeResponseEntity = basicDataService.findStoreByCode(deliverBaseOrder.getStoreCode(), deliverBaseOrder.getApplyType());
        if (storeResponseEntity.getStatusCode().isError() || Objects.isNull(storeResponseEntity.getBody())) {
            return Tips.warn("查询门店信息失败");
        }
        Store store = (Store) storeResponseEntity.getBody();
        //距离换算
        //BigDecimal distance = Distance.getDistance(store.getStorePosition().getLat(), store.getStorePosition().getLng(), deliverBaseOrder.getLat(), deliverBaseOrder.getLng());

        ElemeCreateOrderRequest.ElemeCreateRequestData requestData = new ElemeCreateOrderRequest.ElemeCreateRequestData();
        //设置门店编码
        requestData.setChainStoreCode(store.getStoreCode());
        //配送地址信息
        ElemeCreateOrderRequest.TransportInfo transportInfo = new ElemeCreateOrderRequest.TransportInfo();
        transportInfo.setAddress(store.getStoreAddress());
        transportInfo.setLatitude(store.getStorePosition().getLat());
        transportInfo.setLongitude(store.getStorePosition().getLng());
        transportInfo.setName(store.getStoreName());
        transportInfo.setRemark("");
        transportInfo.setTel(store.getStorePhone());
        transportInfo.setPositionSource(coordinateSystem.getPositionSource());
        requestData.setTransportInfo(transportInfo);

        //收货人
        ElemeCreateOrderRequest.ReceiverInfo receiverInfo = new ElemeCreateOrderRequest.ReceiverInfo();
        receiverInfo.setAddress(deliverBaseOrder.getAddress());
        receiverInfo.setCityCode("0731");
        receiverInfo.setCityName("长沙市");
        receiverInfo.setLatitude(new BigDecimal(deliverBaseOrder.getLat()));
        receiverInfo.setLongitude(new BigDecimal(deliverBaseOrder.getLng()));
        receiverInfo.setName(deliverBaseOrder.getReceiveUser());
        //如果转换 门店位置和收货人位置都转换
        receiverInfo.setPositionSource(coordinateSystem.getPositionSource());
        receiverInfo.setPrimaryPhone(deliverBaseOrder.getContactPhone());
        requestData.setReceiverInfo(receiverInfo);

        requestData.setGoodsCount(deliverBaseOrder.getDeliverOrderProductList().size());
        //重量计算 所有商品的份数*重量*基础重量
        deliverBaseOrder.getDeliverOrderProductList().forEach(item ->
                requestData.setOrderWeight(requestData.getOrderWeight().add(new BigDecimal(Calculator.mul(Calculator.mul(item.getProductQty(), item.getStandardQty()), item.getBaseWeight()))))
        );
        requestData.setIfNeedAgentPayment(0);//不需要代购
        requestData.setIfNeedInvoiced(0);//不需要发票
        //此处设置商品列表
        ElemeCreateOrderRequest.ItemsJson[] goodsItems = new ElemeCreateOrderRequest.ItemsJson[deliverBaseOrder.getDeliverOrderProductList().size()];
        for (int i = 0; i < deliverBaseOrder.getDeliverOrderProductList().size(); i++) {
            DeliverOrderProduct deliverOrderProduct = deliverBaseOrder.getDeliverOrderProductList().get(i);
            ElemeCreateOrderRequest.ItemsJson goodsItem = new ElemeCreateOrderRequest.ItemsJson();
            goodsItem.setName(deliverOrderProduct.getProductName());
            goodsItem.setPrice(new BigDecimal(Calculator.div(deliverOrderProduct.getPrice(), 100.0)));
            goodsItem.setActualPrice(new BigDecimal(Calculator.div(deliverOrderProduct.getDiscountPrice(), 100.0)));
            goodsItem.setIfNeedAgentPurchase(0);
            goodsItem.setIfNeedPackage(0);
            goodsItem.setQuantity(deliverOrderProduct.getProductQty());
            goodsItems[i] = goodsItem;
        }
        requestData.setItemsJson(goodsItems);

        requestData.setNotifyUrl(deliverBaseOrder.getBackUrl());//设置回调地址
        requestData.setOrderActualAmount(new BigDecimal(Calculator.div(deliverBaseOrder.getAmountPayable(), 100.0)));//应付订单金额
        requestData.setOrderAddTime(System.currentTimeMillis());
        requestData.setOrderPaymentMethod(1);
        requestData.setOrderRemark(deliverBaseOrder.getRemark());
        requestData.setOrderPaymentStatus(1);//已支付
        requestData.setOrderTotalAmount(new BigDecimal(Calculator.div(deliverBaseOrder.getTotalAmount(), 100.0)));//订单总金额
        requestData.setOrderType(1);

        requestData.setPartnerOrderCode(deliverBaseOrder.getHdOrderCode());

        try {
            String response = fengNiaoClient.deliver(requestData, this.getFengniaoAccessTokenFromCache());
            //发送蜂鸟返回结果
            FengniaoOrderAddResult fengniaoOrderAddResult = Jackson.object(response, FengniaoOrderAddResult.class);
            log.info("蜂鸟配送返回结果{}", fengniaoOrderAddResult);
            if (fengniaoOrderAddResult.getCode() == 200) {
                //记录配送信息
                DeliverNote deliverNote = new DeliverNote();
                deliverNote.setOrderId(deliverBaseOrder.getOrderId());
                deliverNote.setOrderCode(deliverBaseOrder.getOrderCode());//订单号
                deliverNote.setDeliverCode(deliverBaseOrder.getHdOrderCode());//配送单号与海鼎订单号一致
                deliverNote.setDeliverType(DeliverType.FENGNIAO);
                deliverNote.setStoreCode(deliverBaseOrder.getStoreCode());
                deliverNote.setRemark(deliverBaseOrder.getRemark());
                deliverNote.setFee(deliverBaseOrder.getDeliveryFee());//自己传递过来的配送费
                deliverNote.setDistance(distance.doubleValue());//自己计算的直线距离
                //创建配送单
                deliveryNoteService.createNewDeliverNote(deliverNote);
                //更新配送信息
                DeliverNote updateDeliverNote = new DeliverNote();
                updateDeliverNote.setDeliverStatus(DeliveryStatus.UNRECEIVE);
                updateDeliverNote.setId(deliverNote.getId());
                deliveryNoteService.updateById(updateDeliverNote);
                //写入配送订单流程表
                deliverBaseOrderService.create(deliverBaseOrder);
                return Tips.info("创建蜂鸟配送成功");
            }
        } catch (IOException e) {
            log.error("蜂鸟发送配送失败,{}", e);
            return Tips.warn("蜂鸟发送配送失败");
        }
        return Tips.warn("创建蜂鸟配送失败");
    }


    /**
     * 取消配送订单原因列表
     *
     * @return string
     */
    public String cancelOrderReasons() {
        return "{\"result\":[\n" +
                "{\"code\":1,\"desc\":\"物流原因：订单长时间未分配骑手\"},\n" +
                "{\"code\":2,\"desc\":\"物流原因：分配骑手后，骑手长时间未取件\"}, \n" +
                "{\"code\":3,\"desc\":\"物流原因：骑手告知不配送，让取消订单\"}, \n" +
                "{\"code\":4,\"desc\":\"商品缺货/无法出货/已售完\"}, \n" +
                "{\"code\":5,\"desc\":\"商户联系不上门店/门店关门了\"}, \n" +
                "{\"code\":6,\"desc\":\"商户发错单\"}, \n" +
                "{\"code\":7,\"desc\":\"商户/顾客自身定位错误\"}, \n" +
                "{\"code\":8,\"desc\":\"商户改其他第三方配送\"}, \n" +
                "{\"code\":9,\"desc\":\"顾客下错单/临时不想要了\"}, \n" +
                "{\"code\":10,\"desc\":\"顾客自取/不在家/要求另改时间配送\"}\n" +
                " ]}";
    }

    /**
     * @param hdOrderCode    订单hdCode
     * @param cancelReasonId 取消原因id
     * @param cancelReason   取消原因说明
     * @return Tips
     */
    public Tips cancel(String hdOrderCode, int cancelReasonId, String cancelReason) {
        DeliverNote searchDeliverNote = deliveryNoteService.selectByDeliverCode(hdOrderCode);
        if (Objects.isNull(searchDeliverNote)) {
            return Tips.of(-1, "未找到配送单信息");
        }

        ElemeCancelOrderRequest.ElemeCancelOrderRequstData elemeCancelOrderRequstData = new ElemeCancelOrderRequest.ElemeCancelOrderRequstData();
        elemeCancelOrderRequstData.setPartnerOrderCode(hdOrderCode);
        elemeCancelOrderRequstData.setOrderCancelReasonCode(2);
        elemeCancelOrderRequstData.setOrderCancelCode(cancelReasonId);
        elemeCancelOrderRequstData.setOrderCancelDescription(cancelReason);
        elemeCancelOrderRequstData.setOrderCancelTime(System.currentTimeMillis());
        //elemeCancelOrderRequstData.setOrderCancelNotifyUrl("http://172.16.10.203:8211/thirdparty-service-v1-0/delivery/fengniao/callback");

        try {
            String cancel = fengNiaoClient.cancel(elemeCancelOrderRequstData, this.getFengniaoAccessTokenFromCache());
            log.error("取消蜂鸟配送,{}", cancel);
            //修改数据库中记录
            searchDeliverNote.setFailureCause(cancelReason);
            searchDeliverNote.setCancelTime(new Date());
            searchDeliverNote.setDeliverStatus(DeliveryStatus.FAILURE);
            deliveryNoteService.updateById(searchDeliverNote);
            return Tips.of(1, "取消蜂鸟配送成功");
        } catch (IOException e) {
            log.error("取消蜂鸟配送失败,{e}", e);
            return Tips.of(-1, "取消蜂鸟配送失败");
        }

    }

    /**
     * 蜂鸟回调处理
     *
     * @param backMsg 回调JSON参数字符串
     * @return Tips
     */
    public Tips callback(String backMsg) {
        Map<String, Object> backMap = Jackson.map(backMsg);
        String dataBody = null;
        try {
            dataBody = URLDecoder.decode(backMap.get("data").toString(), StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            log.error("不支持的字符转换格式");
        }
        Map<String, Object> backDataMap = Jackson.map(dataBody);
        // 返回的订单编码
        String deliverCode = backDataMap.get("partner_order_code") + "";

        DeliverNote deliverNote = deliveryNoteService.selectByDeliverCode(deliverCode);

        if (Objects.isNull(deliverNote)) {
            log.error("未找到蜂鸟配送回调订单{}" + deliverCode);
            return Tips.of(-1, "未找到蜂鸟配送回调订单" + deliverCode);
        }
        // 修改蜂鸟为最新状态
        int fengniaoCallbackStatus = Integer.valueOf(String.valueOf(backDataMap.get("order_status")));

        switch (fengniaoCallbackStatus) {
            case 1:
                // 系统已接单
                log.info("系统已接单");
                deliverNote.setDeliverStatus(DeliveryStatus.WAIT_GET);
                deliveryNoteService.updateById(deliverNote);
                break;
            case 20:
                // 已分配骑手
                // 修改订单状态为配送中
                log.info("已分配骑手");
                deliverNote.setDeliverName((String) backDataMap.get("carrier_driver_name"));
                deliverNote.setDeliverPhone((String) backDataMap.get("carrier_driver_phone"));
                deliverNote.setReceiveTime(new Date());
                deliverNote.setDeliverStatus(DeliveryStatus.WAIT_GET);
                deliveryNoteService.updateById(deliverNote);
                break;
            case 80:
                // 骑手已到店
                log.info("骑手已到店");
                break;
            case 2:
                // 配送中
                deliverNote.setDeliverStatus(DeliveryStatus.DELIVERING);
                deliveryNoteService.updateById(deliverNote);
                //修改订单状态为配送中
                //orderService.delivering(deliverNote.getOrderId());
                break;
            case 3:
                // 已送达
                // 修改订单状态为已收货
                log.info("已送达");
                deliverNote.setDeliverStatus(DeliveryStatus.DONE);
                deliveryNoteService.updateById(deliverNote);
                //修改订单状态为已收货
                //orderService.received(deliverNote.getOrderId());
                break;
            case 5:
                log.error("系统拒单");
                // 修改配送信息为失败
                deliverNote.setDeliverStatus(DeliveryStatus.FAILURE);
                deliverNote.setFailureCause(backDataMap.get("description").toString());
                deliverNote.setCancelTime(new Date());
                deliveryNoteService.updateById(deliverNote);
                break;
            default:
                log.error("蜂鸟回调返回其他状态");
                break;
        }
        return Tips.of(1, "配送回调处理成功");
    }

    /**
     * @param hdOrderCode 订单code
     * @return JSON String
     */
    public String detail(String hdOrderCode) {
        ElemeQueryOrderRequest.ElemeQueryRequestData elemeQueryRequestData = new ElemeQueryOrderRequest.ElemeQueryRequestData();
        elemeQueryRequestData.setPartnerOrderCode(hdOrderCode);
        try {
            return fengNiaoClient.one(elemeQueryRequestData, this.getFengniaoAccessTokenFromCache());
        } catch (IOException e) {
            log.error("查询蜂鸟订单详情失败,{e}", e);
            return "查询蜂鸟订单详情失败";
        }
    }

    /**
     * 从缓存中获取蜂鸟accessToken
     *
     * @return TokenResponse
     */
    private TokenResponse getFengniaoAccessTokenFromCache() {

        RBucket<TokenResponse> accessToken = redissonClient.getBucket("com:lhiot:oc:delivery:fengniao:access-token");
        TokenResponse tokenResponse = accessToken.get();
        //如果是空，说明是还没刷新或者已经失效,则缓存一个小时
        if (Objects.isNull(tokenResponse)) {
            tokenResponse = fengNiaoClient.accessToken();
            accessToken.setAsync(tokenResponse, 1, TimeUnit.HOURS);
        }

        return tokenResponse;
    }

}
