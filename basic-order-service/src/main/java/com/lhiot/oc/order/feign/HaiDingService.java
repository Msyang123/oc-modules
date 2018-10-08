package com.lhiot.oc.order.feign;

import com.lhiot.oc.order.model.HaiDingOrderParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

/**
 * @author zhangfeng created in 2018/9/29 9:19
 **/
@FeignClient("hai-ding-service-v1-0")
@Component
public interface HaiDingService {


    @RequestMapping(value = "/hd/order/{orderCode}/cancel",method = RequestMethod.PUT)
    ResponseEntity hdCancel(@PathVariable("orderCode") String orderCode, @RequestParam("reason") String reason);

    @RequestMapping(value = "/hd/order/refund",method = RequestMethod.PUT)
    ResponseEntity hdRefund(@RequestBody HaiDingOrderParam haiDingOrderParam);

    @RequestMapping(value = "/hd/inventory",method = RequestMethod.PUT)
    ResponseEntity reduce(@RequestBody HaiDingOrderParam haiDingOrderParam);
}
