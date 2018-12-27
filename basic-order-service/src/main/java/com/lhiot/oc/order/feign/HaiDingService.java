package com.lhiot.oc.order.feign;

import com.lhiot.oc.order.model.HaiDingOrderParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author zhangfeng created in 2018/9/29 9:19
 **/
@FeignClient("thirdparty-service-v1-0")
public interface HaiDingService {


    @RequestMapping(value = "/hd/order/{orderCode}/cancel", method = RequestMethod.PUT)
    ResponseEntity<String> hdCancel(@PathVariable("orderCode") String orderCode, @RequestParam("reason") String reason);

    @RequestMapping(value = "/hd/order/refund", method = RequestMethod.PUT)
    ResponseEntity<String> hdRefund(@RequestBody HaiDingOrderParam haiDingOrderParam);

    @RequestMapping(value = "/hd/inventory", method = RequestMethod.PUT)
    ResponseEntity<String> reduce(@RequestBody HaiDingOrderParam haiDingOrderParam);
}
