package com.lhiot.oc.delivery.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


@FeignClient("basic-order-service-v1-0")
public interface OrderService {

    //修改订单为配送中
    @RequestMapping(value = "/orders/{orderId}/delivering", method = RequestMethod.PUT)
    ResponseEntity delivering(@PathVariable("orderId") Long orderId);

    //修改订单为已收货
    @RequestMapping(value = "/orders/{orderId}/received", method = RequestMethod.PUT)
    ResponseEntity received(@PathVariable("orderId") Long orderId);

}
