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
    public Tips send(CoordinateSystem coordinate, Store store, DeliverOrderParam deliverOrderParam, Long deliverNoteId) {
        double distance = this.distance(store, deliverOrderParam,coordinate);
        if (Calculator.gt(distance, FeeCalculator.MAX_DELIVERY_RANGE)) {
            return Tips.warn("超过配送范围！");
        }
        CreateOrderByShopRequest request = createOrderByShopRequest(coordinate, deliverNoteId, deliverOrderParam);
        try {
            String response = client.deliver(request);
            CreateOrderResponse added = Jackson.object(response, CreateOrderResponse.class);
            if (Objects.equals(added.getCode(), "0")) {
                return Tips.info("发送美团配送成功").data(this.deliverNote(deliverOrderParam,distance, DeliverType.MEITUAN));
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

    private CreateOrderByShopRequest createOrderByShopRequest(CoordinateSystem coordinate, Long deliverNoteId, DeliverOrderParam deliverOrderParam) {
        CreateOrderByShopRequest createOrderByShopRequest = new CreateOrderByShopRequest();
        createOrderByShopRequest.setDeliveryId(deliverNoteId);
        createOrderByShopRequest.setOrderId(deliverOrderParam.getHdOrderCode());
        createOrderByShopRequest.setShopId(deliverOrderParam.getStoreCode());//测试环境为"test_0001" 正式环境要换成真正的门店编码 deliverOrderParam.getStoreCode()
        //配送服务代码，详情见合同
        //飞速达:4002
        //快速达:4011
        //及时达:4012
        //集中送:4013
        createOrderByShopRequest.setDeliveryServiceCode(4011);

        createOrderByShopRequest.setReceiverName(deliverOrderParam.getReceiveUser());
        createOrderByShopRequest.setReceiverAddress(deliverOrderParam.getAddress());
        createOrderByShopRequest.setReceiverPhone(deliverOrderParam.getContactPhone());
        createOrderByShopRequest.setReceiverLng((int) (deliverOrderParam.getLng() * 1000000));
        createOrderByShopRequest.setReceiverLat((int) (deliverOrderParam.getLat() * 1000000));

        createOrderByShopRequest.setCoordinateType(coordinate.getPositionSource() == 2 ? 1 : 0);//	坐标类型，0：火星坐标（高德，腾讯地图均采用火星坐标） 1：百度坐标 （默认值为0）
        createOrderByShopRequest.setGoodsValue(BigDecimal.valueOf(Calculator.round(deliverOrderParam.getTotalAmount() / 100.0, 2)));//分转元,四舍五入精确两位小数
        //重量计算 所有商品的份数*重量*基础重量
        deliverOrderParam.getDeliverOrderProductList().forEach(item ->
                createOrderByShopRequest.setGoodsWeight(createOrderByShopRequest.getGoodsWeight().add(BigDecimal.valueOf(item.getTotalWeight())))
        );
        createOrderByShopRequest.setGoodsWeight(createOrderByShopRequest.getGoodsWeight().setScale(2, RoundingMode.UP));

        //具体商品信息
        OpenApiGoods openApiGoods = new OpenApiGoods();
        List<OpenApiGood> openApiGoodList = new ArrayList<>(deliverOrderParam.getDeliverOrderProductList().size());
        deliverOrderParam.getDeliverOrderProductList().forEach(item -> {
            OpenApiGood openApiGood = new OpenApiGood();
            openApiGood.setGoodCount(item.getProductQty());
            openApiGood.setGoodName(item.getProductName());
            openApiGood.setGoodPrice(BigDecimal.valueOf(item.getDiscountPrice()));
            openApiGoodList.add(openApiGood);
        });
        openApiGoods.setGoods(openApiGoodList);
        createOrderByShopRequest.setGoodsDetail(openApiGoods);

        createOrderByShopRequest.setGoodsPickupInfo(deliverOrderParam.getRemark());
        createOrderByShopRequest.setGoodsDeliveryInfo(deliverOrderParam.getRemark());

        DeliverTime deliverTime = deliverOrderParam.getDeliverTime();

        createOrderByShopRequest.setExpectedPickupTime(deliverTime.getStartTime().getTime());
        createOrderByShopRequest.setExpectedDeliveryTime(deliverTime.getEndTime().getTime());

        //依据订单配送时间来决定是及时单还是预约单
        Date today = DateTime.convert(LocalDate.now().atTime(LocalTime.parse("23:59:59")));
        if (deliverTime.getStartTime().after(today)) {
            createOrderByShopRequest.setOrderType(OrderType.PREBOOK.getCode());//订单类型，目前只支持预约单0: 及时单(尽快送达，限当日订单)1: 预约单
        } else {
            createOrderByShopRequest.setOrderType(OrderType.NORMAL.getCode());
        }
        int hdOrderCodeLength = deliverOrderParam.getHdOrderCode().length();
        createOrderByShopRequest.setPoiSeq(deliverOrderParam.getHdOrderCode().substring(hdOrderCodeLength - 4, hdOrderCodeLength));//骑手取货单号
        createOrderByShopRequest.setNote(deliverOrderParam.getRemark());
        return createOrderByShopRequest;
    }
}
