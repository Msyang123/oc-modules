package com.lhiot.oc.basic.feign;

import com.lhiot.oc.basic.domain.enums.ApplicationTypeEnum;
import com.lhiot.oc.basic.feign.domain.Store;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;


@Component
@FeignClient("data-service-v1-0")
public interface BaseDataServiceFeign {

    //查询门店信息
    @RequestMapping(value = "/stores/{storeId}", method = RequestMethod.GET)
    ResponseEntity<Store> findStoresId(@PathVariable("storeId") Long storeId, @RequestParam("applicationType") ApplicationTypeEnum applicationType);
}
