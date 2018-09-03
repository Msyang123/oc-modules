package com.lhiot.oc.basic.api;

import com.leon.microx.common.wrapper.Tips;
import com.leon.microx.util.Jackson;
import com.leon.microx.util.StringUtils;
import com.lhiot.order.domain.BaseOrderInfo;
import com.lhiot.order.domain.OrderAssortment;
import com.lhiot.order.domain.OrderProduct;
import com.lhiot.order.domain.enums.DelayExchange;
import com.lhiot.order.domain.enums.OrderStatus;
import com.lhiot.order.domain.enums.RefundStatus;
import com.lhiot.order.domain.inparam.ReturnOrderParam;
import com.lhiot.order.service.BaseOrderService;
import com.lhiot.order.service.Delivery.DeliveryService;
import com.lhiot.order.service.payment.PaymentLogService;
import com.lhiot.order.service.payment.PaymentService;
import com.lhiot.order.service.payment.WeChatUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestController
@Api("海鼎回调api")
@RequestMapping("/hd")
public class HdApi {

    private final BaseOrderService baseOrderService;
    private final DeliveryService deliveryService;
    private final PaymentLogService paymentLogService;
    private final PaymentService paymentService;
    final private WeChatUtil weChatUtil;
    private final RabbitTemplate rabbit;

    @Autowired
    public HdApi(BaseOrderService baseOrderService, DeliveryService deliveryService, PaymentLogService paymentLogService, PaymentService paymentService, WeChatUtil weChatUtil, RabbitTemplate rabbit) {
        this.baseOrderService = baseOrderService;
        this.deliveryService = deliveryService;
        this.paymentLogService = paymentLogService;
        this.paymentService = paymentService;
        this.weChatUtil = weChatUtil;
        this.rabbit = rabbit;
    }

    @ApiOperation("海鼎回调处理")
    @PostMapping("/call/back")
    public void hdCallback(@RequestBody String hdResult) {
        {
            log.info("海鼎回调");
            try {
                log.info("传入JSON字符串：" + hdResult);

                Map<String, Object> map = Jackson.map(hdResult);
                Map<String, Object> contentMap = (Map<String, Object>) map.get("content");

                log.info("进行转义后的字符串 resultStr " + hdResult);
                log.info("content = " + contentMap.toString());
                String code = (String) contentMap.get("front_order_id");
                // 依据订单编码查询订单  根据海鼎code查询  add Limiaojun by 20180804
                BaseOrderInfo order = baseOrderService.findByHdCode(code, true, true, true, false, false);
                // 所有订单推送类消息
                if ("order".equals(map.get("group")) && Objects.nonNull(order)) {
                    // 订单备货
                    if ("order.shipped".equals(map.get("topic"))) {
                        log.info("订单备货回调********");

                        BaseOrderInfo updateOrder = new BaseOrderInfo();
                        updateOrder.setId(order.getId());


                        updateOrder.setHdStockAt(new Timestamp(System.currentTimeMillis()));
                        switch (order.getReceivingWay()) {
                            //门店自提
                            case TO_THE_STORE:

                                updateOrder.setStatus(OrderStatus.RECEIVED);
                                updateOrder.setRecieveTime(new Timestamp(System.currentTimeMillis()));//配送完成时间
                                break;
                            //送货上门
                            case TO_THE_HOME:
                                updateOrder.setStatus(OrderStatus.SEND_OUT);
                                //发送到物流配送
                                rabbit.convertAndSend(DelayExchange.SEND_DELIVERY_DELAY.getExchangeName(), DelayExchange.DelayQueues.SEND_DELIVERY_DELAY_TIMEOUT.getDelayName(),
                                        Jackson.json(order), message -> {
                                            //延迟时间为预定配送时间-当前系统时间 如果配送规定时间超时 1秒后发送到配送延迟队列
                                            long leftDeliveryTime=(order.getDeliveryTime().getTime()-System.currentTimeMillis()<=0)?1000:order.getDeliveryTime().getTime()-System.currentTimeMillis();
                                            message.getMessageProperties().setExpiration(String.valueOf(leftDeliveryTime));
                                            return message;
                                        });
                                break;
                            default:
                                break;
                        }
                        baseOrderService.update(updateOrder);
                    }// 可能海鼎返回结果已经改了
                    else if ("return.received".equals(map.get("topic"))) {
                        log.info("订单退货回调*********");
                        if (Objects.equals(OrderStatus.RETURNING, order.getStatus())) {
                            Long baseUserId = order.getPaymentLog().getUserId();
                            ReturnOrderParam returnOrderParam = new ReturnOrderParam();
                            returnOrderParam.setUserId(baseUserId);
                            returnOrderParam.setOrderId(order.getId());
                            //已退款的订单商品
                            List<String> barcodeIdList = order.getOrderProducts().parallelStream()
                                    .filter(orderProduct -> Objects.equals(orderProduct.getRefundStatus(), RefundStatus.REFUND))
                                    .map(OrderProduct::getBarcode)
                                    .map(String::valueOf).collect(Collectors.toList());
                            String barcodeIds = StringUtils.arrayToDelimitedString(StringUtils.toStringArray(barcodeIdList), ",");

                            List<String> standardIdList = order.getOrderProducts().parallelStream()
                                    .filter(orderProduct -> Objects.equals(orderProduct.getRefundStatus(), RefundStatus.REFUND))
                                    .map(OrderProduct::getStandardId)
                                    .map(String::valueOf).collect(Collectors.toList());
                            String standardIds = StringUtils.arrayToDelimitedString(StringUtils.toStringArray(standardIdList), ",");

                            List<String> assortmentIdList = order.getOrderAssortment().parallelStream()
                                    .filter(orderAssortment -> Objects.equals(orderAssortment.getRefundStatus(), RefundStatus.REFUND))
                                    .map(OrderAssortment::getAssortmentId)
                                    .map(String::valueOf).collect(Collectors.toList());
                            String assortmentIds = StringUtils.arrayToDelimitedString(StringUtils.toStringArray(assortmentIdList), ",");
                            returnOrderParam.setOrderBarcodeIds(barcodeIds);
                            returnOrderParam.setAssortmentIds(assortmentIds);
                            returnOrderParam.setOrderProductIds(standardIds);
                            returnOrderParam.setReturnReason(order.getReason());
                            Tips tips = paymentService.refundOrder(returnOrderParam);


                            //判断退款是否成功，成功则写日志
                            if (Objects.nonNull(tips) && Objects.equals(tips.getCode(), "1")) {
                                BaseOrderInfo orderInfo = new BaseOrderInfo();
                                orderInfo.setStatus(OrderStatus.ALREADY_RETURN);
                                orderInfo.setId(order.getId());
                                baseOrderService.updateOrderStatusById(orderInfo);

                            } else {
                                log.error("订单退货海鼎回调处理", tips);
                            }
                            //todo 发统一广播通知
                            //XXXTODO 鲜果师订单 发送队列，减提成和发送模板消息
/*                            if(Apply.FRUIT_DOCTOR.equals(applicationType)){
                            	order.setAmountPayable(partGoodFee);
                            	order.setOrderStatus(OrderStatus.ALREADY_RETURN);
                            }*/
                        } else {
                            log.error("退款失败 订单状态：", order.getStatus());
                        }
                    }
                }
            } catch (Exception e) {
                log.info("message = " + e.getMessage());
            }
        }
    }

}
