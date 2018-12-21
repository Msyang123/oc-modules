package com.lhiot.oc.order.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author zhangfeng create in 10:39 2018/12/4
 */
@FeignClient("payment-service-v1-0")
@Component
public interface PaymentService {

    /**
     * 支付完成，修改支付日志状态为PAID
     * @param payId 支付Id
     * @param paidModel 支付信息
     * @return ResponseEntity
     */
    @RequestMapping(value = "/records/{outTradeNo}/completed", method = RequestMethod.PUT)
    ResponseEntity updatePaymentLog(@PathVariable("outTradeNo") String payId, @RequestBody PaidModel paidModel);

    /**
     * 支付退款
     * @param payId 支付Id
     * @param refundParam 退款信息
     * @return ResponseEntity
     */
    @RequestMapping(value = "/paid/{outTradeNo}/refunds", method = RequestMethod.POST)
    ResponseEntity refund(@PathVariable("outTradeNo") String payId, @RequestBody RefundParam refundParam);


    @RequestMapping(value = "/records/{outTradeNo}", method = RequestMethod.GET)
    ResponseEntity findPaymentLog(@PathVariable("outTradeNo") String outTradeNo);
}
