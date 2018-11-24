package com.lhiot.oc.delivery.client;

import com.leon.microx.util.Calculator;
import com.leon.microx.util.DateTime;
import com.leon.microx.util.Jackson;
import com.leon.microx.util.Maps;
import com.leon.microx.web.result.Tips;
import com.lhiot.oc.delivery.api.calculator.FeeCalculator;
import com.lhiot.oc.delivery.client.meituan.MeiTuanClient;
import com.lhiot.oc.delivery.client.meituan.model.*;
import com.lhiot.oc.delivery.entity.DeliverNote;
import com.lhiot.oc.delivery.feign.Store;
import com.lhiot.oc.delivery.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * @author Leon (234239150@qq.com) created in 11:42 18.11.13
 */
@Slf4j
public class MeiTuanAdapter implements AdaptableClient {

    private MeiTuanClient client;

    public MeiTuanAdapter(MeiTuanClient client) {
        this.client = client;
    }

    @Override
    public Tips send(CoordinateSystem coordinate, Store store, DeliverOrder deliverOrder, Long deliverNoteId) {
        double distance = this.distance(store, deliverOrder,coordinate);
        if (Calculator.gt(distance, FeeCalculator.MAX_DELIVERY_RANGE)) {
            return Tips.warn("超过配送范围！");
        }
        CreateOrderByShopRequest request = createOrderByShopRequest(coordinate, deliverNoteId, deliverOrder);
        try {
            String response = client.deliver(request);
            CreateOrderResponse added = Jackson.object(response, CreateOrderResponse.class);
            if (Objects.equals(added.getCode(), "0")) {
                return Tips.info("发送美团配送成功").data(this.createDeliverNote(distance, deliverOrder));
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return Tips.error("发送美团配送出错 - " + e.getMessage(), e);
        }
        return Tips.warn("发送美团配送失败");
    }

    @Override
    public Tips cancel(DeliverNote deliverNote, CancelReason reason) {
        CancelOrderRequest cancelOrderRequest = new CancelOrderRequest();
        cancelOrderRequest.setCancelOrderReasonId(CancelOrderReasonId.findByCode(reason.getId().intValue()));
        cancelOrderRequest.setCancelReason(reason.getReason());
        cancelOrderRequest.setDeliveryId(deliverNote.getId());
        cancelOrderRequest.setMtPeisongId(deliverNote.getExt());
        try {
            String cancel = client.cancel(cancelOrderRequest);
            log.error("取消美团配送,{}", cancel);
            return Tips.info("取消成功");
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return Tips.error("取消美团配送出错 - " + e.getMessage(), e);
        }
    }

    @Override
    public Tips cancelReasons() {
        List<Map<String, Object>> reasons = new ArrayList<>(10);
        reasons.add(Maps.of("code", 199, "desc", "其他接入方原因"));
        reasons.add(Maps.of("code", 299, "desc", "其他美团配送原因"));
        reasons.add(Maps.of("code", 399, "desc", "其他原因"));
        return Tips.info("本地返回").data(Maps.of("result", reasons));
    }

    @Override
    public Tips deliverNoteDetail(DeliverNote deliverNote) {
        QueryOrderRequest queryOrderRequest = new QueryOrderRequest();
        queryOrderRequest.setDeliveryId(deliverNote.getId());
        queryOrderRequest.setMtPeisongId(deliverNote.getExt());
        try {
            String json = client.one(queryOrderRequest);
            return Tips.info("查询成功").data(json);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return Tips.error("美团查询配送单失败 - " + e.getMessage(), e);
        }
    }

    @Override
    public Tips backSignature(Map<String,String> backParam){
        return Objects.equals(backParam.get("sign"),client.backSignature(backParam))?Tips.of(HttpStatus.OK,"验证成功"):Tips.of(HttpStatus.BAD_REQUEST,"验证错误");
    }

    private DeliverNote createDeliverNote(double distance, DeliverOrder deliverOrder) {
        DeliverNote deliverNote = new DeliverNote();
        deliverNote.setOrderCode(deliverOrder.getOrderCode());//订单号
        deliverNote.setDeliverCode(deliverOrder.getHdOrderCode());//配送单单号
        deliverNote.setDeliverType(DeliverType.MEITUAN);
        deliverNote.setStoreCode(deliverOrder.getStoreCode());
        deliverNote.setRemark(deliverOrder.getRemark());
        deliverNote.setFee(deliverOrder.getDeliveryFee());//自己传递过来的配送费
        deliverNote.setDistance(distance);//自己计算的直线距离
        return deliverNote;
    }

    private CreateOrderByShopRequest createOrderByShopRequest(CoordinateSystem coordinate, Long deliverNoteId, DeliverOrder deliverOrder) {
        CreateOrderByShopRequest createOrderByShopRequest = new CreateOrderByShopRequest();
        createOrderByShopRequest.setDeliveryId(deliverNoteId);
        createOrderByShopRequest.setOrderId(deliverOrder.getHdOrderCode());
        createOrderByShopRequest.setShopId(deliverOrder.getStoreCode());//测试环境为"test_0001" 正式环境要换成真正的门店编码 deliverOrder.getStoreCode()
        //配送服务代码，详情见合同
        //飞速达:4002
        //快速达:4011
        //及时达:4012
        //集中送:4013
        createOrderByShopRequest.setDeliveryServiceCode(4011);

        createOrderByShopRequest.setReceiverName(deliverOrder.getReceiveUser());
        createOrderByShopRequest.setReceiverAddress(deliverOrder.getAddress());
        createOrderByShopRequest.setReceiverPhone(deliverOrder.getContactPhone());
        createOrderByShopRequest.setReceiverLng((int) (deliverOrder.getLng() * 1000000));
        createOrderByShopRequest.setReceiverLat((int) (deliverOrder.getLat() * 1000000));

        createOrderByShopRequest.setCoordinateType(coordinate.getPositionSource() == 2 ? 1 : 0);//	坐标类型，0：火星坐标（高德，腾讯地图均采用火星坐标） 1：百度坐标 （默认值为0）
        createOrderByShopRequest.setGoodsValue(BigDecimal.valueOf(Calculator.round(deliverOrder.getTotalAmount() / 100.0, 2)));//分转元,四舍五入精确两位小数
        //重量计算 所有商品的份数*重量*基础重量
        deliverOrder.getDeliverOrderProductList().forEach(item ->
                createOrderByShopRequest.setGoodsWeight(createOrderByShopRequest.getGoodsWeight().add(BigDecimal.valueOf(Calculator.mul(Calculator.mul(item.getProductQty(), item.getStandardQty()), item.getBaseWeight()))))
        );
        createOrderByShopRequest.setGoodsWeight(createOrderByShopRequest.getGoodsWeight().setScale(2, RoundingMode.UP));

        //具体商品信息
        OpenApiGoods openApiGoods = new OpenApiGoods();
        List<OpenApiGood> openApiGoodList = new ArrayList<>(deliverOrder.getDeliverOrderProductList().size());
        deliverOrder.getDeliverOrderProductList().forEach(item -> {
            OpenApiGood openApiGood = new OpenApiGood();
            openApiGood.setGoodCount(item.getProductQty());
            openApiGood.setGoodName(item.getProductName());
            openApiGood.setGoodPrice(BigDecimal.valueOf(item.getDiscountPrice()));
            openApiGoodList.add(openApiGood);
        });
        openApiGoods.setGoods(openApiGoodList);
        createOrderByShopRequest.setGoodsDetail(openApiGoods);

        createOrderByShopRequest.setGoodsPickupInfo(deliverOrder.getRemark());
        createOrderByShopRequest.setGoodsDeliveryInfo(deliverOrder.getRemark());

        DeliverTime deliverTime = deliverOrder.getDeliverTime();

        createOrderByShopRequest.setExpectedPickupTime(deliverTime.getStartTime().getTime());
        createOrderByShopRequest.setExpectedDeliveryTime(deliverTime.getEndTime().getTime());

        //依据订单配送时间来决定是及时单还是预约单
        Date today = DateTime.convert(LocalDate.now().atTime(LocalTime.parse("23:59:59")));
        if (deliverTime.getStartTime().after(today)) {
            createOrderByShopRequest.setOrderType(OrderType.PREBOOK.getCode());//订单类型，目前只支持预约单0: 及时单(尽快送达，限当日订单)1: 预约单
        } else {
            createOrderByShopRequest.setOrderType(OrderType.NORMAL.getCode());
        }
        int hdOrderCodeLength = deliverOrder.getHdOrderCode().length();
        createOrderByShopRequest.setPoiSeq(deliverOrder.getHdOrderCode().substring(hdOrderCodeLength - 4, hdOrderCodeLength));//骑手取货单号
        createOrderByShopRequest.setNote(deliverOrder.getRemark());
        return createOrderByShopRequest;
    }
}
