package com.lhiot.oc.delivery.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Component
@FeignClient("basic-data-service-v1-0")
public interface BasicDataService {

    //查询门店信息
    @RequestMapping(value = "/stores/{storeId}", method = RequestMethod.GET)
    ResponseEntity<Store> findStoreById(@PathVariable("storeId") Long storeId, @RequestParam("applicationType") String applicationType);

    @RequestMapping(value = "/stores/code/{code}", method = RequestMethod.GET)
    ResponseEntity<Store> findStoreByCode(@PathVariable("code") String code, @RequestParam("applicationType") String applicationType);
}
