package com.lhiot.oc.payment.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;


/**
 * 基础数据中心基础服务
 */
@FeignClient("basic-data-service-v1-0")
public interface BaseDataService {

	/**
	 * 根据支付商户名称简称查询支付签名信息
	 */
    @GetMapping("/payment-config")
    ResponseEntity<PaymentConfig> findPaymentConfig(@RequestParam("name") String name);

}
