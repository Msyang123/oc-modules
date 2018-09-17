package com.lhiot.oc.basic.service;

import com.leon.microx.support.result.Tips;
import com.leon.microx.util.Calculator;
import com.leon.microx.util.Jackson;
import com.lhiot.oc.basic.domain.DeliverBaseOrder;
import com.lhiot.oc.basic.domain.DeliverNote;
import com.lhiot.oc.basic.domain.enums.DeliverNeedConver;
import com.lhiot.oc.basic.domain.enums.DeliverType;
import com.lhiot.oc.basic.domain.enums.DeliveryStatus;
import com.lhiot.oc.basic.feign.BaseOrderServiceFeign;
import com.lhiot.oc.basic.feign.ThirdPartyServiceFeign;
import com.lhiot.oc.basic.feign.domain.DadaOrderAddResult;
import com.lhiot.oc.basic.feign.domain.OrderParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@Transactional
public class DadaDeliveryService implements IDelivery {

    private final DeliveryNoteService deliveryNoteService;
    private final ThirdPartyServiceFeign thirdPartyServiceFeign;
    private final BaseOrderServiceFeign baseOrderServiceFeign;
    private final DeliverBaseOrderService deliverBaseOrderService;

    public DadaDeliveryService(DeliveryNoteService deliveryNoteService, ThirdPartyServiceFeign thirdPartyServiceFeign, BaseOrderServiceFeign baseOrderServiceFeign, DeliverBaseOrderService deliverBaseOrderService) {
        this.deliveryNoteService = deliveryNoteService;
        this.thirdPartyServiceFeign = thirdPartyServiceFeign;
        this.baseOrderServiceFeign = baseOrderServiceFeign;
        this.deliverBaseOrderService = deliverBaseOrderService;
    }

    /**
     * 达达配送订单处理
     *
     * @return
     */
    public Tips send(DeliverNeedConver deliverNeedConver,DeliverBaseOrder deliverBaseOrder) {

        log.info("订单达达配送：{}", deliverBaseOrder);

        OrderParam orderParam=new OrderParam();
        //orderParam.setCallback("http://172.16.10.203:8211/thirdparty-service-v1-0/delivery/dada/callback");
        orderParam.setCargoNum(deliverBaseOrder.getGoodsCount());//默认一个商品
        orderParam.setCargoPrice(deliverBaseOrder.getTotalAmount());
        orderParam.setCargoWeight(deliverBaseOrder.getCargoWeight());
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
        ResponseEntity<String> sendToDadaResult = null;
        DadaOrderAddResult dadaOrderAddResult =null;
        if(Objects.equals(deliverNeedConver,DeliverNeedConver.YES)){
            sendToDadaResult = thirdPartyServiceFeign.addOrder(orderParam);
        }else{
            sendToDadaResult = thirdPartyServiceFeign.addOrderNeedConver(orderParam);
        }

        if(Objects.nonNull(sendToDadaResult)&&sendToDadaResult.getStatusCode().is2xxSuccessful()){
            dadaOrderAddResult = Jackson.object(sendToDadaResult.getBody(), DadaOrderAddResult.class);
            log.info("达达配送添加订单返回结果{}",sendToDadaResult.getBody());
            //如果是订单重复需要调用 重新添加达达配送订单接口 reAddOrder
            if(2105 == dadaOrderAddResult.getCode()){
                if(Objects.equals(deliverNeedConver,DeliverNeedConver.YES)){
                    sendToDadaResult = thirdPartyServiceFeign.reAddOrder(orderParam);
                }else{
                    sendToDadaResult = thirdPartyServiceFeign.reAddOrderNeedConver(orderParam);
                }
                if(Objects.nonNull(sendToDadaResult)&&sendToDadaResult.getStatusCode().is2xxSuccessful()) {
                    dadaOrderAddResult = Jackson.object(sendToDadaResult.getBody(), DadaOrderAddResult.class);
                }else{
                    return Tips.of(-1,"重复发送达达订单失败");
                }
            }
        }else{
            return Tips.of(-1,"发送达达订单失败");
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
        return thirdPartyServiceFeign.cancelOrderReasons().getBody();
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
        ResponseEntity<String> cancelResult= thirdPartyServiceFeign.cancel(hdOrderCode,cancelReasonId,cancelReason);
       if(cancelResult.getStatusCode().is2xxSuccessful()){
           //TODO 修改数据库中记录
           DeliverNote deliverNote=new DeliverNote();
           deliverNote.setFailureCause(cancelReason);
           deliverNote.setCancelTime(new Date());
           deliverNote.setDeliverStatus(DeliveryStatus.FAILURE);
           deliveryNoteService.updateById(deliverNote);
           return Tips.of(1, "取消成功");
       }else{
           log.error("达达取消配送订单调用第三方失败：{}", cancelResult.getBody());
           return Tips.of(-1, "达达取消配送订单调用第三方失败：" + cancelResult.getBody());
       }
    }

    public Tips callBack(String backMsg) {
        Map<String,String> resultMap=Jackson.object(backMsg,Map.class);

        String deliverCode = resultMap.get("order_id");

        DeliverNote deliverNote = deliveryNoteService.selectByDeliverCode(deliverCode);

        if(Objects.isNull(deliverNote)){
            log.error("未找到达达配送回调订单{}" + deliverCode);
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
                baseOrderServiceFeign.transfering(deliverNote.getOrderId());
                break;
            // 配送完成
            case 4:
                deliverNote.setDeliverStatus(DeliveryStatus.DONE);
                deliveryNoteService.updateById(deliverNote);
                //修改订单状态为已收货
                baseOrderServiceFeign.received(deliverNote.getOrderId());
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
        ResponseEntity<String> responseEntity = thirdPartyServiceFeign.getOrder(hdOrderCode);
        if(Objects.nonNull(responseEntity)&&responseEntity.getStatusCode().is2xxSuccessful()){
            return responseEntity.getBody();
        }
        return "查询达达配送信息失败";
    }
}
