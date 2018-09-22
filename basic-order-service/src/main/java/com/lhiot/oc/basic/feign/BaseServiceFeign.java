package com.lhiot.oc.basic.feign;

import com.leon.microx.support.result.Multiple;
import com.lhiot.oc.basic.model.ProductShelfResult;
import com.lhiot.oc.basic.model.Store;
import com.lhiot.oc.basic.model.type.ApplicationType;
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


    @RequestMapping(value = "/products/shelf/list", method = RequestMethod.GET)
    ResponseEntity<Multiple<ProductShelfResult>> findProductByProductIdList(@RequestParam("shelfIds") String shelfIds);

}
