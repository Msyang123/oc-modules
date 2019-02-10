package com.lhiot.oc.delivery.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

@Component
@FeignClient("thirdparty-service-v1-0")
public interface ThirdpartyService {

    @RequestMapping(value = "/hd/order/deliver", method = RequestMethod.PUT)
    ResponseEntity<Store> updateHdStatus(@RequestBody Delivery delivery);

}
