package com.lhiot.oc.basic.feign;

import com.lhiot.oc.basic.model.ApplicationTypeEnum;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


/**
 * 用户中心基础服务
 * Created by yj
 */
@FeignClient("base-data-service-v1-0-0")
@Component
public interface BaseServiceFeign {

    @RequestMapping(value="/{storeId}",method = RequestMethod.GET)
    ResponseEntity storeById(@PathVariable Long storeId, ApplicationTypeEnum applicationTypeEnum);

}
