package com.lhiot.oc.delivery.client;

import com.leon.microx.util.Calculator;
import com.leon.microx.util.Jackson;
import com.leon.microx.util.Maps;
import com.leon.microx.web.result.Tips;
import com.lhiot.oc.delivery.api.calculator.FeeCalculator;
import com.lhiot.oc.delivery.client.fengniao.FengNiaoClient;
import com.lhiot.oc.delivery.client.fengniao.model.ElemeCancelOrderRequest;
import com.lhiot.oc.delivery.client.fengniao.model.ElemeCreateOrderRequest;
import com.lhiot.oc.delivery.client.fengniao.model.ElemeQueryOrderRequest;
import com.lhiot.oc.delivery.client.fengniao.model.OrderAdded;
import com.lhiot.oc.delivery.entity.DeliverNote;
import com.lhiot.oc.delivery.feign.Store;
import com.lhiot.oc.delivery.model.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Leon (234239150@qq.com) created in 9:05 18.11.13
 */
@Slf4j
public class FengNiaoAdapter implements AdaptableClient {

    private static final String ACCESS_TOKEN_CACHE_NAME = "com:lhiot:oc:delivery:fengniao:access-token";

    private FengNiaoClient client;

    public FengNiaoAdapter(FengNiaoClient client) {
        this.client = client;
    }

    @Override
    public Tips send(CoordinateSystem coordinate, Store store, DeliverOrder deliverOrder, Long deliverNoteId) {
        double distance = this.distance(store, deliverOrder);
        if (Calculator.gt(distance, FeeCalculator.MAX_DELIVERY_RANGE)) {
            return Tips.warn("超过配送范围！");
        }
        try {
            String response = client.deliver(this.createRequestData(coordinate, store, deliverOrder), client.accessToken(ACCESS_TOKEN_CACHE_NAME));
            log.info("蜂鸟配送返回结果{}", response);
            OrderAdded added = Jackson.object(response, OrderAdded.class);
            if (added.getCode() == 200) {
                return Tips.info("发送蜂鸟成功").data(this.createDeliverNote(deliverOrder, distance));
            }
            return Tips.warn("发送蜂鸟成功，但返回错误 - " + response);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return Tips.error("发送蜂鸟配送出错 - " + e.getMessage(), e);
        }
    }

    @Override
    public Tips cancel(DeliverNote deliverNote, CancelReason reason) {
        ElemeCancelOrderRequest.ElemeCancelOrderRequstData elemeCancelOrderRequstData = new ElemeCancelOrderRequest.ElemeCancelOrderRequstData();
        elemeCancelOrderRequstData.setPartnerOrderCode(deliverNote.getDeliverCode());
        elemeCancelOrderRequstData.setOrderCancelReasonCode(2);
        elemeCancelOrderRequstData.setOrderCancelCode(reason.getId());
        elemeCancelOrderRequstData.setOrderCancelDescription(reason.getReason());
        elemeCancelOrderRequstData.setOrderCancelTime(System.currentTimeMillis());
        try {
            String cancel = client.cancel(elemeCancelOrderRequstData, client.accessToken(ACCESS_TOKEN_CACHE_NAME));
            log.error("取消蜂鸟配送,{}", cancel);
            return Tips.info("取消成功");
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return Tips.error("蜂鸟取消配送出错 - " + e.getMessage(), e);
        }
    }

    @Override
    public Tips cancelReasons() {
        List<Map<String, Object>> reasons = new ArrayList<>(10);
        reasons.add(Maps.of("code", 1, "desc", "物流原因：订单长时间未分配骑手"));
        reasons.add(Maps.of("code", 2, "desc", "物流原因：分配骑手后，骑手长时间未取件"));
        reasons.add(Maps.of("code", 3, "desc", "物流原因：骑手告知不配送，让取消订单"));
        reasons.add(Maps.of("code", 4, "desc", "商品缺货/无法出货/已售完"));
        reasons.add(Maps.of("code", 5, "desc", "商户联系不上门店/门店关门了"));
        reasons.add(Maps.of("code", 6, "desc", "商户发错单"));
        reasons.add(Maps.of("code", 7, "desc", "商户/顾客自身定位错误"));
        reasons.add(Maps.of("code", 8, "desc", "商户改其他第三方配送"));
        reasons.add(Maps.of("code", 9, "desc", "顾客下错单/临时不想要了"));
        reasons.add(Maps.of("code", 10, "desc", "顾客自取/不在家/要求另改时间配送"));
        return Tips.info("本地返回").data(Maps.of("result", reasons));
    }

    @Override
    public Tips deliverNoteDetail(DeliverNote deliverNote) {
        ElemeQueryOrderRequest.ElemeQueryRequestData elemeQueryRequestData = new ElemeQueryOrderRequest.ElemeQueryRequestData();
        elemeQueryRequestData.setPartnerOrderCode(deliverNote.getDeliverCode());
        try {
            String json = client.one(elemeQueryRequestData, client.accessToken(ACCESS_TOKEN_CACHE_NAME));
            return Tips.info("查询成功").data(json);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return Tips.error("蜂鸟查询配送单失败 - " + e.getMessage(), e);
        }
    }

    private DeliverNote createDeliverNote(DeliverOrder deliverOrder, double distance) {
        DeliverNote deliverNote = new DeliverNote();
        deliverNote.setOrderId(deliverOrder.getOrderId());
        deliverNote.setOrderCode(deliverOrder.getOrderCode());//订单号
        deliverNote.setDeliverCode(deliverOrder.getHdOrderCode());//配送单号与海鼎订单号一致
        deliverNote.setDeliverType(DeliverType.FENGNIAO);
        deliverNote.setStoreCode(deliverOrder.getStoreCode());
        deliverNote.setRemark(deliverOrder.getRemark());
        deliverNote.setFee(deliverOrder.getDeliveryFee());//自己传递过来的配送费
        deliverNote.setDistance(distance);
        return deliverNote;
    }

    private ElemeCreateOrderRequest.ElemeCreateRequestData createRequestData(CoordinateSystem coordinate, Store store, DeliverOrder deliverOrder) {
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
        transportInfo.setPositionSource(coordinate.getPositionSource());
        requestData.setTransportInfo(transportInfo);

        //收货人
        ElemeCreateOrderRequest.ReceiverInfo receiverInfo = new ElemeCreateOrderRequest.ReceiverInfo();
        receiverInfo.setAddress(deliverOrder.getAddress());
        receiverInfo.setCityCode("0731");
        receiverInfo.setCityName("长沙市");
        receiverInfo.setLatitude(BigDecimal.valueOf(deliverOrder.getLat()));
        receiverInfo.setLongitude(BigDecimal.valueOf(deliverOrder.getLng()));
        receiverInfo.setName(deliverOrder.getReceiveUser());
        //如果转换 门店位置和收货人位置都转换
        receiverInfo.setPositionSource(coordinate.getPositionSource());
        receiverInfo.setPrimaryPhone(deliverOrder.getContactPhone());
        requestData.setReceiverInfo(receiverInfo);

        requestData.setGoodsCount(deliverOrder.getDeliverOrderProductList().size());
        //重量计算 所有商品的份数*重量*基础重量
        deliverOrder.getDeliverOrderProductList().forEach(item ->
                requestData.setOrderWeight(requestData.getOrderWeight().add(BigDecimal.valueOf(Calculator.mul(Calculator.mul(item.getProductQty(), item.getStandardQty()), item.getBaseWeight()))))
        );
        requestData.setIfNeedAgentPayment(0);//不需要代购
        requestData.setIfNeedInvoiced(0);//不需要发票
        //此处设置商品列表
        ElemeCreateOrderRequest.ItemsJson[] goodsItems = new ElemeCreateOrderRequest.ItemsJson[deliverOrder.getDeliverOrderProductList().size()];
        for (int i = 0; i < deliverOrder.getDeliverOrderProductList().size(); i++) {
            DeliverProduct deliverOrderProduct = deliverOrder.getDeliverOrderProductList().get(i);
            ElemeCreateOrderRequest.ItemsJson goodsItem = new ElemeCreateOrderRequest.ItemsJson();
            goodsItem.setName(deliverOrderProduct.getProductName());
            goodsItem.setPrice(BigDecimal.valueOf(Calculator.div(deliverOrderProduct.getPrice(), 100.0)));
            goodsItem.setActualPrice(BigDecimal.valueOf(Calculator.div(deliverOrderProduct.getDiscountPrice(), 100.0)));
            goodsItem.setIfNeedAgentPurchase(0);
            goodsItem.setIfNeedPackage(0);
            goodsItem.setQuantity(deliverOrderProduct.getProductQty());
            goodsItems[i] = goodsItem;
        }
        requestData.setItemsJson(goodsItems);

        requestData.setNotifyUrl(deliverOrder.getBackUrl());//设置回调地址
        requestData.setOrderActualAmount(BigDecimal.valueOf(Calculator.div(deliverOrder.getAmountPayable(), 100.0)));//应付订单金额
        requestData.setOrderAddTime(System.currentTimeMillis());
        requestData.setOrderPaymentMethod(1);
        requestData.setOrderRemark(deliverOrder.getRemark());
        requestData.setOrderPaymentStatus(1);//已支付
        requestData.setOrderTotalAmount(BigDecimal.valueOf(Calculator.div(deliverOrder.getTotalAmount(), 100.0)));//订单总金额
        requestData.setOrderType(1);

        requestData.setPartnerOrderCode(deliverOrder.getHdOrderCode());
        return requestData;
    }
}
