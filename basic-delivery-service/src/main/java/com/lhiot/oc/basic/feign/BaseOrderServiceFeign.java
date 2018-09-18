package com.lhiot.oc.basic.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


@Component
@FeignClient("basic-order-service-v1-0")
public interface BaseOrderServiceFeign {

    //修改订单为配送中
    @RequestMapping(value = "/transfering/{orderId}", method = RequestMethod.PUT)
    ResponseEntity transfering(@PathVariable("orderId") Long orderId);

    //修改订单为已收货
    @RequestMapping(value = "/received/{orderId}", method = RequestMethod.PUT)
    ResponseEntity received(@PathVariable("orderId") Long orderId);


}
