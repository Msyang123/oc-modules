package com.lhiot.oc.payment.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;


/**
 * 用户中心基础服务
 */
@FeignClient("basic-user-service-v1-0")
public interface BaseUserService {

    /**
     * 根据业务用户id查询用户
     */
    @GetMapping("/users/{id}")
    ResponseEntity<User> findUser(@PathVariable("id") Long userId);

    /**
     * 更新公共用户鲜果币
     */
    @PutMapping("/users/{id}/balance")
    ResponseEntity updateBalance(@PathVariable("id") long userId, @RequestBody Balance balance);
}
