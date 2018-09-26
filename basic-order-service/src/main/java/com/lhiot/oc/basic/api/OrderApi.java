package com.lhiot.oc.basic.api;

import com.leon.microx.support.result.Tips;
import com.lhiot.oc.basic.event.OrderFlowEvent;
import com.lhiot.oc.basic.feign.BaseServiceFeign;
import com.lhiot.oc.basic.model.*;
import com.lhiot.oc.basic.model.type.AllowRefund;
import com.lhiot.oc.basic.model.type.OrderStatus;
import com.lhiot.oc.basic.service.OrderService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * Zhangfeng created in 2018/9/19 9:42
 **/
@RestController
@Slf4j
@RequestMapping("/orders")
@Transactional
public class OrderApi {

    private OrderService orderService;
    private BaseServiceFeign baseServiceFeign;
    private ApplicationEventPublisher publisher;
    private static final String HD_CANCEL_ORDER_SUCCESS_RESULT_STRING = "{\"success\":true}";

    public OrderApi(OrderService orderService, BaseServiceFeign baseServiceFeign, ApplicationEventPublisher publisher) {
        this.orderService = orderService;
        this.baseServiceFeign = baseServiceFeign;
        this.publisher = publisher;
    }

    @PostMapping("/")
    @ApiOperation("创建订单(套餐)--公共")
    @ApiImplicitParam(paramType = "body", name = "orderParam", dataType = "CreateOrderParam", required = true, value = "创建订单传入参数")
    @Transactional
    public ResponseEntity createOrderWithAssortment(@RequestBody CreateOrderParam orderParam) {

        //验证参数中优惠金额及商品
        Tips backMsg = orderService.validationParam(orderParam);
        if (backMsg.getCode().equals("-1")) {
            return ResponseEntity.badRequest().body(backMsg.getMessage());
        }
        //写库
        OrderDetailResult result = orderService.createOrder(orderParam);
        //写订单流水
        publisher.publishEvent(new OrderFlowEvent(null, result.getStatus(), result.getId()));
        return ResponseEntity.ok(result);
    }

    @ApiOperation("根据订单code查询订单详情")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "orderCode", dataType = "String", required = true, value = "订单code"),
            @ApiImplicitParam(paramType = "query", name = "needProductList", dataType = "boolean", required = true, value = "是否需要加载商品信息"),
            @ApiImplicitParam(paramType = "query", name = "needOrderFlowList", dataType = "boolean", required = true, value = "是否需要加载订单操作流水信息")
    })
    @GetMapping("/{orderCode}")
    public ResponseEntity orderDetail(@PathVariable("orderCode") String orderCode, @RequestParam("needProductList") boolean needProductList,
                                      @RequestParam("needOrderFlowList") boolean needOrderFlowList) {
        OrderDetailResult order = orderService.findByCode(orderCode, needProductList, needOrderFlowList);
        if (Objects.isNull(order)) {
            return ResponseEntity.badRequest().body(Tips.of(-1, "获取订单失败"));
        }
        return ResponseEntity.ok(order);
    }

    @ApiOperation("取消订单")
    @ApiImplicitParam(paramType = "path", name = "orderCode", value = "订单Code", required = true, dataType = "String")
    @PutMapping("/{orderCode}/cancel")
    public ResponseEntity cancelOrder(@PathVariable("orderCode") String orderCode) {
        OrderDetailResult orderDetailResult = orderService.findByCode(orderCode);
        if (Objects.isNull(orderDetailResult)) {
            return ResponseEntity.badRequest().body("订单不存在");
        }
        if (!Objects.equals(orderDetailResult.getStatus(), OrderStatus.WAIT_PAYMENT)) {
            return ResponseEntity.badRequest().body(orderDetailResult.getStatus().getDescription() + "状态不可取消订单");
        }
        BaseOrder baseOrder = new BaseOrder();
        baseOrder.setCode(orderCode);
        baseOrder.setStatus(OrderStatus.FAILURE);
        int result = orderService.updateOrderStatusByCode(baseOrder);
        if (result > 0) {
            publisher.publishEvent(new OrderFlowEvent(orderDetailResult.getStatus(), OrderStatus.FAILURE, orderDetailResult.getId()));
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().body("更新订单状态为失效失败");
    }

    @ApiOperation("订单退货(包括部分和全部)")
    @ApiImplicitParam(paramType = "path", name = "orderCode", value = "订单orderCode", required = true, dataType = "String")
    @PutMapping("/{orderCode}/refund")
    public ResponseEntity refundOrder(@PathVariable("orderCode") String orderCode,@NotNull @RequestBody ReturnOrderParam returnOrderParam) {
        OrderDetailResult searchBaseOrderInfo = orderService.findByCode(orderCode);
        if (Objects.isNull(searchBaseOrderInfo)) {
            return ResponseEntity.badRequest().body("未找到订单");
        }
        if (Objects.equals(searchBaseOrderInfo.getAllowRefund(), AllowRefund.NO)) {
            return ResponseEntity.badRequest().body("订单未非允许退货订单");
        }
        //只允许待发货 已发货 退货中的订单退货
        if (!Objects.equals(searchBaseOrderInfo.getStatus(), OrderStatus.WAIT_SEND_OUT) &&
                !Objects.equals(searchBaseOrderInfo.getStatus(), OrderStatus.SEND_OUT) &&
                !Objects.equals(searchBaseOrderInfo.getStatus(), OrderStatus.RECEIVED)) {
            return ResponseEntity.badRequest().body("只允许待发货/已发货的订单退货，当前订单状态为:" + searchBaseOrderInfo.getStatus().getDescription());
        }
        orderService.refundOrderByCode(searchBaseOrderInfo, returnOrderParam);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{orderCode}/dispatching")
    @ApiOperation("修改订单为配送中")
    public ResponseEntity dispatching(@PathVariable("orderCode") String orderCode) {
        OrderDetailResult orderDetailResult = orderService.findByCode(orderCode);
        if (Objects.isNull(orderDetailResult)) {
            return ResponseEntity.badRequest().body("订单不存在");
        }
        if (!Objects.equals(orderDetailResult.getStatus(), OrderStatus.SEND_OUT)) {
            return ResponseEntity.badRequest().body(orderDetailResult.getStatus().getDescription() + "状态不可进行配送");
        }
        BaseOrder baseOrder = new BaseOrder();
        baseOrder.setCode(orderCode);
        baseOrder.setStatus(OrderStatus.DISPATCHING);
        int result = orderService.updateOrderStatusByCode(baseOrder);
        if (result > 0) {
            publisher.publishEvent(new OrderFlowEvent(orderDetailResult.getStatus(), OrderStatus.DISPATCHING, orderDetailResult.getId()));
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().body("更新订单状态为配送中失败");
    }

    @ApiOperation("修改订单为已收货")
    @PutMapping(value = "/{orderCode}/received")
    public ResponseEntity received(@PathVariable("orderCode") String orderCode) {
        OrderDetailResult orderDetailResult = orderService.findByCode(orderCode);
        if (Objects.isNull(orderDetailResult)) {
            return ResponseEntity.badRequest().body("订单不存在");
        }
        if (!Objects.equals(orderDetailResult.getStatus(), OrderStatus.WAIT_SEND_OUT) && !Objects.equals(orderDetailResult.getStatus(), OrderStatus.DISPATCHING)) {
            return ResponseEntity.badRequest().body(orderDetailResult.getStatus().getDescription() + "状态不可更改为已收货");
        }
        BaseOrder baseOrder = new BaseOrder();
        baseOrder.setCode(orderCode);
        baseOrder.setStatus(OrderStatus.RECEIVED);
        int result = orderService.updateOrderStatusByCode(baseOrder);
        if (result > 0) {
            publisher.publishEvent(new OrderFlowEvent(orderDetailResult.getStatus(), OrderStatus.RECEIVED, orderDetailResult.getId()));
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().body("更新订单状态为已收货失败");
    }


    @ApiOperation("海鼎订单调货")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "orderCode", value = "调货订单编码", dataType = "String", required = true),
            @ApiImplicitParam(paramType = "query", name = "storeId", value = "调货目标门店id", dataType = "Long", required = true),
            @ApiImplicitParam(paramType = "query", name = "operationUser", value = "操作人", dataType = "String", required = true)
    })
    @PutMapping("/{orderCode}/store")
    public ResponseEntity modifyStoreInOrder(@PathVariable("orderCode") String orderCode, @RequestParam Long storeId, @RequestParam String operationUser) {
        OrderDetailResult searchBaseOrderInfo = orderService.findByCode(orderCode);
        if (Objects.isNull(searchBaseOrderInfo)) {
            return ResponseEntity.badRequest().body("订单不存在！");
        }
        if (!Objects.equals(OrderStatus.WAIT_SEND_OUT, searchBaseOrderInfo.getStatus()) || !Objects.equals(HdStatus.SEND_OUT, searchBaseOrderInfo.getHdStatus())) {
            log.info("订单状态：" + searchBaseOrderInfo.getStatus() + "----海鼎状态：" + searchBaseOrderInfo.getHdStatus());
            return ResponseEntity.badRequest().body("当前订单状态不可调货！");
        }
        //远程查找调货门店 不需要查询门店位置
        ResponseEntity<Store> storeInfoResponseEntity = baseServiceFeign.findStoreById(storeId, null);
        if (storeInfoResponseEntity == null || storeInfoResponseEntity.getStatusCode().isError()) {
            log.info("远程查找调货门店查询失败：{}", storeId);
            return ResponseEntity.badRequest().body("远程查找调货门店查询失败，请重试！");
        }
        Store storeInfo = storeInfoResponseEntity.getBody();
        if (storeInfo == null) {
            log.info("远程查找调货门店查询未找到门店：{}", storeId);
            return ResponseEntity.badRequest().body("远程查找调货门店查询未找到门店，请重试！");
        }

        //TODO 发送海鼎取消订单 基于当前的 HdOrderCode
        ResponseEntity<String> hdResponse = null;//thirdPartyServiceFeign.hdOrderCancel(orderInfo.getHdOrderCode(), "海鼎调货");
        if (Objects.isNull(hdResponse) || !Objects.equals(HD_CANCEL_ORDER_SUCCESS_RESULT_STRING, hdResponse.getBody())) {
            log.info("海鼎取消订单编号为：" + searchBaseOrderInfo.getHdOrderCode());
            return ResponseEntity.badRequest().body("海鼎取消订单失败，请重试！");
        }

        //TODO 发送海鼎新的门店订单信息 是否需要校验库存 待定
        ResponseEntity hdReduceResponse = null; //thirdPartyServiceFeign.hdReduce(orderInfo);
        if (hdReduceResponse == null || hdReduceResponse.getStatusCode().isError()) {
            //TODO 此处需要重试或者其他方式处理
            return ResponseEntity.badRequest().body("海鼎发送失败！");
        }
        //修改订单hdCode以及添加调货门店信息
        int result = orderService.changeStore(storeInfo, operationUser, searchBaseOrderInfo.getId());
        if (result > 0) {
            return ResponseEntity.ok().body("调货成功");
        }
        return ResponseEntity.badRequest().body("调货失败");
    }
}
