package com.lhiot.oc.order.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient("basic-user-service-v1-0")
@Component
public interface UserService {

    @RequestMapping(value = "/users/{id}", method = RequestMethod.GET)
    ResponseEntity<User> findUserById(@PathVariable("id") Long userId);
}
