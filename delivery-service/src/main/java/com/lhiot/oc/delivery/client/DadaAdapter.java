package com.lhiot.oc.delivery.client;

import com.leon.microx.util.Calculator;
import com.leon.microx.util.Jackson;
import com.leon.microx.web.result.Tips;
import com.lhiot.oc.delivery.api.calculator.FeeCalculator;
import com.lhiot.oc.delivery.client.dada.DadaClient;
import com.lhiot.oc.delivery.client.dada.model.OrderAdded;
import com.lhiot.oc.delivery.client.dada.model.OrderParam;
import com.lhiot.oc.delivery.entity.DeliverNote;
import com.lhiot.oc.delivery.feign.Store;
import com.lhiot.oc.delivery.model.CancelReason;
import com.lhiot.oc.delivery.model.CoordinateSystem;
import com.lhiot.oc.delivery.model.DeliverOrderParam;
import com.lhiot.oc.delivery.model.DeliverType;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;

/**
 * 达达客户端适配
 *
 * @author Leon (234239150@qq.com) created in 11:40 18.11.11
 */
@Slf4j
public class DadaAdapter implements AdaptableClient {

    private final DadaClient client;

    public DadaAdapter(DadaClient client) {
        this.client = client;
    }

    @Override
    public Tips send(CoordinateSystem coordinate, Store store, DeliverOrderParam deliverOrderParam, Long deliverNoteId) {
        double distance = this.distance(store, deliverOrderParam,coordinate);
        if (Calculator.gt(distance, FeeCalculator.MAX_DELIVERY_RANGE)) {
            return Tips.warn("超过配送范围！");
        }
        OrderParam orderParam = this.createDeliverParam(deliverOrderParam);
        //在测试环境，使用统一商户和门店进行发单。其中，商户id：73753，门店编号：11047059
        OrderAdded added;
        try {
            String result = client.deliver(orderParam, coordinate.isNeedConvert());
            added = Jackson.object(result, OrderAdded.class);
            log.info("达达配送添加订单返回结果{}", result);
            //如果是订单重复需要调用 重新添加达达配送订单接口 reAddOrder
            if (2105 == added.getCode()) {
                result = client.redeliver(orderParam, coordinate.isNeedConvert());
                added = Jackson.object(result, OrderAdded.class);
                if (added.getCode() != 0) {
                    return Tips.warn("重复发送达达订单失败");
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return Tips.error("达达发送配送失败 - " + e.getMessage(), e);
        }
        return Tips.info("发送达达成功").data(this.deliverNote(deliverOrderParam,distance,DeliverType.DADA));
    }

    @Override
    public Tips cancel(DeliverNote deliverNote, CancelReason reason) {
        try {
            String cancel = client.cancel(deliverNote.getDeliverCode(), reason.getId(), reason.getReason());
            log.error("取消dada配送,{}", cancel);
            return Tips.info("取消成功");
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return Tips.error("达达取消配送失败 - " + e.getMessage(), e);
        }
    }

    @Override
    public Tips cancelReasons() {
        try {
            String json = client.cancelReasons();
            return Tips.info("查询成功").data(json);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return Tips.error("达达查询取消配送原因列表失败 - " + e.getMessage(), e);
        }
    }

    @Override
    public Tips deliverNoteDetail(DeliverNote deliverNote) {
        try {
            String json = client.one(deliverNote.getDeliverCode());
            return Tips.info("查询成功").data(json);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return Tips.error("达达查询配送单失败 - " + e.getMessage(), e);
        }
    }

    @Override
    public Tips backSignature(Map<String,String> backParam){
        boolean verifyResult = client.inspect(backParam.get("client_id"),backParam.get("order_id"),backParam.get("update_time"),backParam.get("signature"));
        return verifyResult?Tips.info("验证成功"):Tips.error("验证失败",new IllegalArgumentException());
    }

    private OrderParam createDeliverParam(DeliverOrderParam deliverOrderParam) {
        OrderParam orderParam = new OrderParam();
        orderParam.setBackUrl(deliverOrderParam.getBackUrl()); // 设置业务回调地址
        orderParam.setCargoNum(deliverOrderParam.getDeliverOrderProductList().size());
        orderParam.setCargoPrice(deliverOrderParam.getAmountPayable()+ deliverOrderParam.getDeliveryFee());//订单实付金额（包括配送费）
        // 重量计算 所有商品的份数*重量*基础重量
        deliverOrderParam.getDeliverOrderProductList().forEach(item ->
                orderParam.setCargoWeight(
                        Calculator.add(orderParam.getCargoWeight(), item.getTotalWeight())
                )
        );
        orderParam.setCityCode("0731");
        orderParam.setInfo(deliverOrderParam.getRemark());
        orderParam.setLat(deliverOrderParam.getLat());
        orderParam.setLng(deliverOrderParam.getLng());
        orderParam.setOriginId(deliverOrderParam.getHdOrderCode());
        orderParam.setOriginMark("lhiot");
        orderParam.setOriginMarkNo(deliverOrderParam.getApplyType().replaceAll("_",""));
        orderParam.setReceiverAddress(deliverOrderParam.getAddress());
        orderParam.setReceiverName(deliverOrderParam.getReceiveUser());
        orderParam.setReceiverPhone(deliverOrderParam.getContactPhone());
        orderParam.setShopNo(deliverOrderParam.getStoreCode());
        return orderParam;
    }
}
