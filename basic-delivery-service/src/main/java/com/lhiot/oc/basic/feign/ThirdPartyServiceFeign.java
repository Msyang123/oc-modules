package com.lhiot.oc.basic.feign;

import com.lhiot.oc.basic.feign.domain.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;


@Component
@FeignClient("3rd-party-services")
public interface ThirdPartyServiceFeign {

    //添加达达配送订单 默认腾讯坐标系不需要转换
    @RequestMapping(value = "/delivery/dada/addOrder", method = RequestMethod.POST)
    ResponseEntity<String> addOrder(@RequestBody OrderParam orderParam);

    //添加达达配送订单 需要转成高德系标准 百度坐标系需要，腾讯坐标系不需要
    @RequestMapping(value = "/delivery/dada/addOrder/needConver", method = RequestMethod.POST)
    ResponseEntity<String> addOrderNeedConver(@RequestBody OrderParam orderParam);

    //重新添加达达配送订单 默认腾讯坐标系不需要转换
    @RequestMapping(value = "/delivery/dada/reAddOrder", method = RequestMethod.POST)
    ResponseEntity<String> reAddOrder(@RequestBody OrderParam orderParam);

    //重新添加达达配送订单 需要转成高德系标准 百度坐标系需要，腾讯坐标系不需要
    @RequestMapping(value = "/delivery/dada/reAddOrder/needConver", method = RequestMethod.POST)
    ResponseEntity<String> reAddOrderNeedConver(@RequestBody OrderParam orderParam);

    //查询达达配送订单 默认腾讯坐标系不需要转换
    @RequestMapping(value = "/delivery/dada/queryFee", method = RequestMethod.POST)
    ResponseEntity<String> queryDeliverFee(@RequestBody OrderParam orderParam);

    //查询达达配送订单 需要转成高德系标准 百度坐标系需要，腾讯坐标系不需要
    @RequestMapping(value = "/delivery/dada/queryFee/needConver", method = RequestMethod.POST)
    ResponseEntity<String> queryDeliverFeeNeedConver(@RequestBody OrderParam orderParam);


    //达达取消原因列表
    @RequestMapping(value = "/delivery/dada/cancel/reasons", method = RequestMethod.GET)
    ResponseEntity<String> cancelOrderReasons();

    //达达取消
    @RequestMapping(value = "/delivery/dada/cancel/{orderId}/{cancelReasonId}/{cancelReason}", method = RequestMethod.GET)
    ResponseEntity<String> cancel(@RequestParam("orderId") String orderId, @RequestParam("cancelReasonId") int cancelReasonId,
                                   @RequestParam("cancelReason") String cancelReason);

    //达达配送单详细查询
    @RequestMapping(value = "/delivery/dada/getOrder/{orderId}", method = RequestMethod.GET)
    ResponseEntity<String> getOrder(@RequestParam("orderId") String orderId);


    //达达投诉原因列表
    @RequestMapping(value = "/delivery/dada/complain/reasons", method = RequestMethod.GET)
    ResponseEntity<String> complainReasons();

    //达达投诉
    @RequestMapping(value = "/delivery/dada/complain/{orderId}/{complainReasonId}", method = RequestMethod.GET)
    ResponseEntity<String> reasons(@RequestParam("orderId") String orderId, @RequestParam("complainReasonId") int complainReasonId);

    //达达门店详细信息
    @RequestMapping(value = "/delivery/dada/shopDetail/{originShopId}", method = RequestMethod.GET)
    ResponseEntity<String> shopDetail(@RequestParam("originShopId") String originShopId);


    /****************蜂鸟配送*******************************************************************/


    //添加蜂鸟配送订单
    @RequestMapping(value = "/delivery/fengniao/add", method = RequestMethod.POST)
    ResponseEntity<String> addOrder(@RequestBody ElemeCreateOrderRequest.ElemeCreateRequestData orderParam);

    //查询蜂鸟配送订单
    @RequestMapping(value = "/delivery/fengniao/get", method = RequestMethod.POST)
    ResponseEntity<String> getOrder(@RequestBody ElemeQueryOrderRequest.ElemeQueryRequestData orderParam);


    //取消蜂鸟配送订单
    @RequestMapping(value = "/delivery/fengniao/cancel", method = RequestMethod.POST)
    ResponseEntity<String> cancel(@RequestBody ElemeCancelOrderRequest.ElemeCancelOrderRequstData orderParam);


    //投诉蜂鸟配送订单
    @RequestMapping(value = "/delivery/fengniao/complaint", method = RequestMethod.POST)
    ResponseEntity<String> orderComplaint(@RequestBody ElemeOrderComplaintRequest.ElemeOrderComplaintRequstData orderParam);

    //蜂鸟取消原因列表
    @RequestMapping(value = "/delivery/fengniao/cancel/reasons", method = RequestMethod.GET)
    ResponseEntity<String> cancelFengniaoOrderReasons();
}
