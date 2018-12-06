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

    @RequestMapping(value = "/pay-logs/{outTradeNo}/completed",method = RequestMethod.PUT)
    ResponseEntity updatePaymentLog(@PathVariable("outTradeNo") String payId, @RequestBody Payed payed);

    @RequestMapping(value = "/pay-logs/{outTradeNo}/refund",method = RequestMethod.PUT)
    ResponseEntity refund(@PathVariable("outTradeNo") String payId,@RequestBody RefundParam refundParam);
}
