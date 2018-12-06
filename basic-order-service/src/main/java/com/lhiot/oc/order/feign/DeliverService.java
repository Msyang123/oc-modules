package com.lhiot.oc.order.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

/**
 * @author zhangfeng create in 10:01 2018/12/5
 */
@FeignClient("delivery-service-v1-0")
@Component
public interface DeliverService {

    @RequestMapping(value = "/{deliverType}/delivery-notes",method = RequestMethod.POST)
    ResponseEntity create(@PathVariable("deliverType") String deliverType, @RequestParam("coordinate") String coordinate, @RequestBody DeliverOrder deliverOrder);
}
