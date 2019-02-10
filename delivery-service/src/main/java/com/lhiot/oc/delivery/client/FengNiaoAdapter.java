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
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Leon (234239150@qq.com) created in 9:05 18.11.13
 */
@Slf4j
public class FengNiaoAdapter implements AdaptableClient {

    private static final String ACCESS_TOKEN_CACHE_NAME = "com:lhiot:oc:delivery:fengniao:access-token";
    private static final int AMAP = 3;//表示使用高德坐标系

    private FengNiaoClient client;

    public FengNiaoAdapter(FengNiaoClient client) {
        this.client = client;
    }

    @Override
    public Tips send(CoordinateSystem coordinate, Store store, DeliverOrderParam deliverOrderParam, Long deliverNoteId) {
        double distance = this.distance(store, deliverOrderParam, coordinate);
        if (Calculator.gt(distance, FeeCalculator.MAX_DELIVERY_RANGE)) {
            return Tips.warn("超过配送范围！");
        }
        try {
            String response = client.deliver(this.createRequestData(coordinate, store, deliverOrderParam), client.accessToken(ACCESS_TOKEN_CACHE_NAME));
            log.info("蜂鸟配送返回结果{}", response);
            OrderAdded added = Jackson.object(response, OrderAdded.class);
            if (added.getCode() == 200) {
                return Tips.info("发送蜂鸟成功").data(this.deliverNote(deliverOrderParam, distance,DeliverType.FENGNIAO));
            }
            return Tips.warn("发送蜂鸟成功，但返回错误 - " + response);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return Tips.error("发送蜂鸟配送出错 - " + e.getMessage(), e);
        }
    }

    @Override
    public Tips cancel(DeliverNote deliverNote, CancelReason reason) {
        ElemeCancelOrderRequest.ElemeCancelOrderRequstData elemeCancelOrderRequestData = new ElemeCancelOrderRequest.ElemeCancelOrderRequstData();
        elemeCancelOrderRequestData.setPartnerOrderCode(deliverNote.getDeliverCode());
        elemeCancelOrderRequestData.setOrderCancelReasonCode(2);
        elemeCancelOrderRequestData.setOrderCancelCode(reason.getId());
        elemeCancelOrderRequestData.setOrderCancelDescription(reason.getReason());
        elemeCancelOrderRequestData.setOrderCancelTime(System.currentTimeMillis());
        try {
            String cancel = client.cancel(elemeCancelOrderRequestData, client.accessToken(ACCESS_TOKEN_CACHE_NAME));
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

    @Override
    public Tips backSignature(Map<String, String> backParam) {
        String appId = backParam.get("app_id");
        String data = backParam.get("data");
        String signature = backParam.get("signature");
        String salt = backParam.get("salt");
        String calcSignature = client.backSignature(appId, data, salt, client.accessToken(ACCESS_TOKEN_CACHE_NAME));
        log.info("---获取到的signature：" + signature);
        log.info("---得到的calcSignature：" + calcSignature);
        // 判断签名是否相等
        return Objects.equals(calcSignature, signature) ? Tips.of(HttpStatus.OK, "验证成功") : Tips.of(HttpStatus.BAD_REQUEST, "验证错误");
    }

    private ElemeCreateOrderRequest.ElemeCreateRequestData createRequestData(CoordinateSystem coordinate, Store store, DeliverOrderParam deliverOrderParam) {
        ElemeCreateOrderRequest.ElemeCreateRequestData requestData = new ElemeCreateOrderRequest.ElemeCreateRequestData();
        //设置门店编码
        requestData.setChainStoreCode(store.getStoreCode());
        //取货地址信息
        ElemeCreateOrderRequest.TransportInfo transportInfo = new ElemeCreateOrderRequest.TransportInfo();
        transportInfo.setAddress(store.getStoreAddress());
        transportInfo.setLatitude(store.getLatitude().doubleValue());
        transportInfo.setLongitude(store.getLongitude().doubleValue());
        transportInfo.setName(store.getStoreName());
        transportInfo.setRemark("");
        transportInfo.setTel(store.getStorePhone());
        transportInfo.setPositionSource(CoordinateSystem.AMAP.getPositionSource());
        requestData.setTransportInfo(transportInfo);

        //收货人
        ElemeCreateOrderRequest.ReceiverInfo receiverInfo = new ElemeCreateOrderRequest.ReceiverInfo();
        receiverInfo.setAddress(deliverOrderParam.getAddress());
        receiverInfo.setCityCode("0731");
        receiverInfo.setCityName("长沙市");
        receiverInfo.setLatitude(BigDecimal.valueOf(deliverOrderParam.getLat()));
        receiverInfo.setLongitude(BigDecimal.valueOf(deliverOrderParam.getLng()));
        receiverInfo.setName(deliverOrderParam.getReceiveUser());
        //如果转换 门店位置和收货人位置都转换
        receiverInfo.setPositionSource(coordinate.getPositionSource());
        receiverInfo.setPrimaryPhone(deliverOrderParam.getContactPhone());
        requestData.setReceiverInfo(receiverInfo);

        requestData.setGoodsCount(deliverOrderParam.getDeliverOrderProductList().size());
        //重量计算 所有商品的份数*重量*基础重量
        deliverOrderParam.getDeliverOrderProductList().forEach(item ->
                requestData.setOrderWeight(requestData.getOrderWeight().add(BigDecimal.valueOf(item.getTotalWeight())))
        );
        requestData.setIfNeedAgentPayment(0);//不需要代购
        requestData.setIfNeedInvoiced(0);//不需要发票
        //此处设置商品列表
        ElemeCreateOrderRequest.ItemsJson[] goodsItems = new ElemeCreateOrderRequest.ItemsJson[deliverOrderParam.getDeliverOrderProductList().size()];
        for (int i = 0; i < deliverOrderParam.getDeliverOrderProductList().size(); i++) {
            DeliverProduct deliverOrderProduct = deliverOrderParam.getDeliverOrderProductList().get(i);
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

        requestData.setNotifyUrl(deliverOrderParam.getBackUrl());//设置回调地址
        requestData.setOrderActualAmount(BigDecimal.valueOf(Calculator.div(deliverOrderParam.getAmountPayable()+ deliverOrderParam.getDeliveryFee(), 100.0)));//应付订单金额（包括配送费）
        requestData.setOrderAddTime(System.currentTimeMillis());
        requestData.setOrderPaymentMethod(1);
        requestData.setOrderRemark(deliverOrderParam.getRemark());
        requestData.setOrderPaymentStatus(1);//已支付
        requestData.setOrderTotalAmount(BigDecimal.valueOf(Calculator.div(deliverOrderParam.getTotalAmount(), 100.0)));//订单总金额
        requestData.setOrderType(1);

        requestData.setPartnerOrderCode(deliverOrderParam.getHdOrderCode());
        return requestData;
    }
}
