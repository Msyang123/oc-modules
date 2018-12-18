package com.lhiot.oc.order.feign;

import com.leon.microx.web.result.Tuple;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient("basic-user-service-v1-0")
@Component
public interface UserService {

    @RequestMapping(value = "/users/phone/{phone}/ids",method = RequestMethod.GET)
    ResponseEntity<Tuple<String>> findUsersByPhone(@PathVariable("phone") String phone);
}
