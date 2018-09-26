package com.lhiot.oc.payment.feign;

import com.lhiot.oc.payment.feign.domain.PaymentConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


/**
 * 基础数据中心基础服务
 * Created by yj
 */
@FeignClient("data-service-v1-0")
@Component
public interface BaseDataServerFeign {


	/**
	 * 根据支付商户名称简称查询支付签名信息
	 */
    @RequestMapping(value="/payment/config/by-name/{paymentName}",method = RequestMethod.GET)
    ResponseEntity<PaymentConfig> findPaymentSignByPaymentName(@PathVariable("paymentName") String paymentName);

}
