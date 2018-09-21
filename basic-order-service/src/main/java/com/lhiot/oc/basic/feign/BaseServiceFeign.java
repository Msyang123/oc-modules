package com.lhiot.oc.basic.feign;

import com.lhiot.oc.basic.model.ApplicationTypeEnum;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;


/**
 * 用户中心基础服务
 * Created by yj
 */
@FeignClient("base-data-service-v1-0-0")
@Component
public interface BaseServiceFeign {


    ResponseEntity storeById(Long storeId, ApplicationTypeEnum applicationTypeEnum);

}
