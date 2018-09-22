package com.lhiot.oc.basic.feign;

import com.lhiot.oc.basic.feign.domain.Store;
import com.lhiot.oc.basic.model.ApplicationType;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;


/**
 * 用户中心基础服务
 * Created by yj
 */
@FeignClient("data-service-v1-0")
@Component
public interface BaseServiceFeign {

    @RequestMapping(value = "/stores/{storeId}", method = RequestMethod.GET)
    ResponseEntity<Store> findStoreById(@PathVariable("storeId") Long storeId, @RequestParam("applicationType") ApplicationType applicationType);

    @RequestMapping(value = "/stores/by-code/{code}", method = RequestMethod.GET)
    ResponseEntity<Store> findStoreByCode(@PathVariable("code") String code, @RequestParam("applicationType") ApplicationType applicationType);

}
