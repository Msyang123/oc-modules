package com.lhiot.oc.delivery.service;

import com.leon.microx.support.result.Tips;
import com.leon.microx.util.Calculator;
import com.leon.microx.util.Jackson;
import com.lhiot.oc.delivery.dada.DadaDeliveryClient;
import com.lhiot.oc.delivery.dada.vo.DadaOrderAddResult;
import com.lhiot.oc.delivery.dada.vo.OrderParam;
import com.lhiot.oc.delivery.domain.DeliverBaseOrder;
import com.lhiot.oc.delivery.domain.DeliverNote;
import com.lhiot.oc.delivery.domain.enums.DeliverNeedConver;
import com.lhiot.oc.delivery.domain.enums.DeliverType;
import com.lhiot.oc.delivery.domain.enums.DeliveryStatus;
import com.lhiot.oc.delivery.feign.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@Transactional
public class DadaDeliveryService implements IDelivery {

    private final DeliveryNoteService deliveryNoteService;
    private final OrderService orderService;
    private final DeliverBaseOrderService deliverBaseOrderService;
    private final DadaDeliveryClient dadaClient;

    public DadaDeliveryService(DeliveryNoteService deliveryNoteService, OrderService orderService, DeliverBaseOrderService deliverBaseOrderService,DadaDeliveryClient dadaClient) {
        this.deliveryNoteService = deliveryNoteService;
        this.orderService = orderService;
        this.deliverBaseOrderService = deliverBaseOrderService;
        this.dadaClient = dadaClient;
    }

    /**
     * 达达配送订单处理
     *
     * @return
     */
    public Tips send(DeliverNeedConver deliverNeedConver,DeliverBaseOrder deliverBaseOrder) {

        log.info("订单达达配送：{}", deliverBaseOrder);

        OrderParam orderParam=new OrderParam();
        orderParam.setBackUrl(deliverBaseOrder.getBackUrl());//设置业务回调地址
        orderParam.setCargoNum(deliverBaseOrder.getDeliverOrderProductList().size());
        orderParam.setCargoPrice(deliverBaseOrder.getTotalAmount());
        //重量计算 所有商品的份数*重量*基础重量
        deliverBaseOrder.getDeliverOrderProductList().forEach(item->
            orderParam.setCargoWeight(Calculator.sub(orderParam.getCargoWeight(),Calculator.mul(Calculator.mul(item.getProductQty(),item.getStandardQty()),item.getBaseWeight())))
        );
        orderParam.setCityCode("0731");
        orderParam.setInfo(deliverBaseOrder.getRemark());
        orderParam.setLat(deliverBaseOrder.getLat());
        orderParam.setLng(deliverBaseOrder.getLng());
        orderParam.setOriginId(deliverBaseOrder.getHdOrderCode());
        orderParam.setOriginMark("lhiot");
        orderParam.setOriginMarkNo(deliverBaseOrder.getApplyType().name());
        orderParam.setReceiverAddress(deliverBaseOrder.getAddress());
        orderParam.setReceiverName(deliverBaseOrder.getReceiveUser());
        orderParam.setReceiverPhone(deliverBaseOrder.getContactPhone());
        orderParam.setShopNo(deliverBaseOrder.getStoreCode());

        //在测试环境，使用统一商户和门店进行发单。其中，商户id：73753，门店编号：11047059
        String sendToDadaResult = null;
        DadaOrderAddResult dadaOrderAddResult =null;
        try {
            sendToDadaResult = dadaClient.deliver(orderParam,Objects.equals(deliverNeedConver,DeliverNeedConver.NO));

            dadaOrderAddResult = Jackson.object(sendToDadaResult, DadaOrderAddResult.class);
            log.info("达达配送添加订单返回结果{}",sendToDadaResult);
            //如果是订单重复需要调用 重新添加达达配送订单接口 reAddOrder
            if(2105 == dadaOrderAddResult.getCode()){
                sendToDadaResult = dadaClient.redeliver(orderParam,Objects.equals(deliverNeedConver,DeliverNeedConver.NO));
                dadaOrderAddResult = Jackson.object(sendToDadaResult, DadaOrderAddResult.class);
                if(dadaOrderAddResult.getCode()!=0) {
                    return Tips.of(-1, "重复发送达达订单失败");
                }
            }
        } catch (IOException e) {
            log.error("达达发送配送失败,{}",e);
            return Tips.of(-1,"达达发送配送失败");
        }
        //记录配送信息
        DeliverNote deliverNote = new DeliverNote();
        deliverNote.setOrderId(deliverBaseOrder.getId());
        deliverNote.setOrderCode(deliverBaseOrder.getOrderCode());//订单号
        deliverNote.setDeliverCode(deliverBaseOrder.getHdOrderCode());//配送单号与海鼎订单号一致
        deliverNote.setDeliverType(DeliverType.DADA);
        deliverNote.setStoreCode(deliverBaseOrder.getStoreCode());
        deliverNote.setRemark(deliverBaseOrder.getRemark());
        deliverNote.setFee(Calculator.toInt(Calculator.mul(dadaOrderAddResult.getResult().getDeliverFee(),100.0)));
        deliverNote.setDistance(dadaOrderAddResult.getResult().getDistance());
        deliverNote.setDeliverCode(deliverBaseOrder.getHdOrderCode());//配送单单号
        //创建配送单
        deliveryNoteService.createNewDeliverNote(deliverNote);

        //写入配送订单流程表
        deliverBaseOrderService.create(deliverBaseOrder);

        return Tips.of(1,"创建达达配送成功");
    }

    /**
     * 取消配送订单原因列表
     * @return
     */
    public String cancelOrderReasons(){
        try {
            return dadaClient.cancelReasons();
        } catch (IOException e) {
            log.error("达达配送取消配送订单原因列表失败",e);
            return "达达配送取消配送订单原因列表失败";
        }
    }

    /**
     * @param hdOrderCode      海鼎订单Code
     * @param cancelReasonId 取消原因id
     * @param cancelReason   取消原因说明
     * @Description: 取消达达平台配送订单
     * @return: Tips
     * @Author: yj
     * @Date: 2018/7/19
     */
    public Tips cancel(String hdOrderCode, int cancelReasonId, String cancelReason) {
        DeliverNote searchDeliverNote = deliveryNoteService.selectByDeliverCode(hdOrderCode);
        if(Objects.isNull(searchDeliverNote)){
            return Tips.of(-1,"未找到配送单信息");
        }
        try {
            String cancelResult = dadaClient.cancel(hdOrderCode,cancelReasonId,cancelReason);
            log.info("达达配送取消配送订单{}",cancelResult);
            searchDeliverNote.setFailureCause(cancelReason);
            searchDeliverNote.setCancelTime(new Date());
            searchDeliverNote.setDeliverStatus(DeliveryStatus.FAILURE);
            deliveryNoteService.updateById(searchDeliverNote);
            return Tips.of(1, "取消成功");
        } catch (IOException e) {
            log.error("达达配送取消配送订单失败{}",e);
            return Tips.of(-1,"达达配送取消配送订单失败");
        }
    }

    public Tips callBack(String backMsg) {
        Map<String,String> resultMap=Jackson.object(backMsg,Map.class);

        String deliverCode = resultMap.get("order_id");

        DeliverNote deliverNote = deliveryNoteService.selectByDeliverCode(deliverCode);

        if(Objects.isNull(deliverNote)){
            log.error("未找到达达配送回调订单{}" ,deliverCode);
            return Tips.of(-1,"未找到达达配送回调订单"+deliverCode);
        }

        switch (Integer.valueOf(resultMap.get("order_status"))) {
            // 待接单
            case 1:
                deliverNote.setDeliverStatus(DeliveryStatus.UNRECEIVE);
                deliveryNoteService.updateById(deliverNote);
                break;
            // 待取货
            case 2:
                deliverNote.setDeliverName(resultMap.get("dm_name"));
                deliverNote.setDeliverPhone(resultMap.get("dm_mobile"));
                deliverNote.setReceiveTime(new Date());
                deliverNote.setDeliverStatus(DeliveryStatus.WAIT_GET);
                deliveryNoteService.updateById(deliverNote);
                break;
            // 配送中
            case 3:
                deliverNote.setDeliverStatus(DeliveryStatus.TRANSFERING);
                deliveryNoteService.updateById(deliverNote);
                //修改订单状态为配送中
                orderService.delivering(deliverNote.getOrderId());
                break;
            // 配送完成
            case 4:
                deliverNote.setDeliverStatus(DeliveryStatus.DONE);
                deliveryNoteService.updateById(deliverNote);
                //修改订单状态为已收货
                orderService.received(deliverNote.getOrderId());
                break;
            // 已取消
            case 5:
                // 已过期
            case 7:

                // 系统故障订单发布失败
            case 1000:
                // 修改配送信息为失败
                deliverNote.setDeliverStatus(DeliveryStatus.FAILURE);
                deliverNote.setFailureCause(resultMap.get("cancel_reason"));
                deliverNote.setCancelTime(new Date());
                deliveryNoteService.updateById(deliverNote);
                break;
            default:
                break;
        }
        return Tips.of(1,"配送回调处理成功");
    }

    /**
     * @param hdOrderCode 订单信息
     * @Description: 查询达达订单详情
     * @return: java.lang.String
     * @Author: yj
     * @Date: 2018/7/19
     */
    public String detail(String hdOrderCode) {
        try {
            return dadaClient.one(hdOrderCode);
        } catch (IOException e) {
            log.error("查询达达订单详情失败,{}",e);
            return "查询达达配送信息失败";
        }
    }
}
