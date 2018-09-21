package com.lhiot.oc.delivery.feign;

import com.lhiot.oc.delivery.domain.BaseOrderInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Component
@FeignClient("3rd-party-services")
public interface ThirdPartyServiceFeign {


    @RequestMapping(value = "/hd/order/{orderCode}" ,method = RequestMethod.GET)
    ResponseEntity<Map<String,Object>> hdOrderDetail(@PathVariable("orderCode") String orderCode);

    @RequestMapping(value = "/hd/order/{orderCode}/cancel" ,method = RequestMethod.PUT)
    ResponseEntity<String> hdOrderCancel(@PathVariable("orderCode") String orderCode, @RequestParam("reason") String reason);

    @RequestMapping(value = "/hd/order/refund" ,method = RequestMethod.PUT)
    ResponseEntity<String> hdOrderRefund(@RequestBody BaseOrderInfo orderInfo);

    @RequestMapping(value = "/hd/inventory",method = RequestMethod.PUT)
    ResponseEntity hdReduce(@RequestBody BaseOrderInfo orderInfo);
    
    //查询门店的实际库存
    @RequestMapping(value = "/hd/sku/{storeId}",method = RequestMethod.POST)
    ResponseEntity<Map<String,Object>> querySku(@PathVariable("storeId") String storeId, @RequestBody String[] skuIds);
}
