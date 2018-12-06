package com.lhiot.oc.order.api;

import com.leon.microx.id.Generator;
import com.leon.microx.probe.annotation.Sniffer;
import com.leon.microx.probe.collector.ProbeEventPublisher;
import com.leon.microx.probe.event.ProbeEvent;
import com.leon.microx.util.Maps;
import com.leon.microx.web.result.Tips;
import com.leon.microx.web.result.Tuple;
import com.leon.microx.web.swagger.ApiParamType;
import com.lhiot.oc.order.entity.type.OrderStatus;
import com.lhiot.oc.order.event.OrderFlowEvent;
import com.lhiot.oc.order.feign.BaseServiceFeign;
import com.lhiot.oc.order.feign.HaiDingService;
import com.lhiot.oc.order.feign.Payed;
import com.lhiot.oc.order.mapper.BaseOrderMapper;
import com.lhiot.oc.order.model.CreateOrderParam;
import com.lhiot.oc.order.model.DeliverParam;
import com.lhiot.oc.order.model.OrderDetailResult;
import com.lhiot.oc.order.model.Store;
import com.lhiot.oc.order.model.type.ApplicationType;
import com.lhiot.oc.order.service.OrderService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * zhangfeng created in 2018/9/19 9:42
 **/
@RestController
@Slf4j
@RequestMapping("/orders")
@Transactional
@Validated
public class OrderApi {

    private OrderService orderService;
    private BaseOrderMapper baseOrderMapper;
    private BaseServiceFeign baseServiceFeign;
    private ApplicationEventPublisher publisher;
    private ProbeEventPublisher probeEventPublisher;
    private HaiDingService haiDingService;
    private Generator<Long> generator;
    private static final String HD_CANCEL_ORDER_SUCCESS_RESULT_STRING = "{\"success\":true}";

    public OrderApi(OrderService orderService, BaseOrderMapper baseOrderMapper, BaseServiceFeign baseServiceFeign, ApplicationEventPublisher publisher, ProbeEventPublisher probeEventPublisher, HaiDingService haiDingService, Generator<Long> generator) {
        this.orderService = orderService;
        this.baseOrderMapper = baseOrderMapper;
        this.baseServiceFeign = baseServiceFeign;
        this.publisher = publisher;
        this.probeEventPublisher = probeEventPublisher;
        this.haiDingService = haiDingService;
        this.generator = generator;
    }

    @PostMapping({"/", ""})
    @ApiOperation(value = "创建订单", response = OrderDetailResult.class)
    @ApiImplicitParam(paramType = "body", name = "orderParam", dataType = "CreateOrderParam", required = true, value = "创建订单传入参数")
    @Transactional
    public ResponseEntity createOrderWithAssortment(@RequestBody CreateOrderParam orderParam) {
        //验证参数中优惠金额及商品
        Tips backMsg = orderService.validationParam(orderParam);
        if (backMsg.err()) {
            return ResponseEntity.badRequest().body(backMsg.getMessage());
        }
        //写库
        OrderDetailResult result = orderService.createOrder(orderParam);
        //写订单流水
        publisher.publishEvent(new OrderFlowEvent(null, result.getStatus(), result.getId()));
        return ResponseEntity.ok(result);
    }

    @ApiOperation("支付回调修改订单状态为WAIT_SEND_OUT")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = ApiParamType.PATH, name = "orderCode", value = "订单编号", required = true, dataType = "String"),
            @ApiImplicitParam(paramType = ApiParamType.BODY, name = "payed", value = "支付信息", required = true, dataType = "Payed")
    })
    @PutMapping("/{orderCode}/payed")
    public ResponseEntity waitSendOut(@PathVariable("orderCode") String orderCode, @RequestBody Payed payed) {
        try {
            orderService.updateWaitPaymentToWaitSendOut(orderCode, payed);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.badRequest().body("支付回掉修改状态失败");
        }
        return ResponseEntity.ok().build();
    }

    @ApiOperation("发送海鼎，修改订单状态")
    @ApiImplicitParam(paramType = ApiParamType.PATH, name = "orderCode", value = "订单编号", required = true, dataType = "String")
    @PutMapping("/{orderCode}/hd-status")
    public ResponseEntity sendHd(@PathVariable("orderCode") String orderCode) {
        try {
            orderService.updateWaitSendOutToSendOuting(orderCode);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.badRequest().body("发送海鼎失败");
        }
        return ResponseEntity.ok().build();
    }

    @ApiOperation("海鼎备货回调，送货上门订单修改订单状态为WAIT_DISPATCHING，且发送配送")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = ApiParamType.PATH, name = "orderCode", value = "订单编号", dataType = "String", required = true),
            @ApiImplicitParam(paramType = ApiParamType.BODY, name = "deliverParam", value = "发送配送入参", dataType = "DeliverParam", required = true)
    })
    @PutMapping("/{orderCode}/delivery")
    public ResponseEntity sendDelivery(@PathVariable("orderCode") String orderCode, @RequestBody DeliverParam deliverParam) {
        OrderDetailResult order = orderService.findByCode(orderCode, true, false);
        if (Objects.isNull(order)) {
            return ResponseEntity.badRequest().body("订单不存在");
        }
        try {
            orderService.sendDelivery(order, deliverParam);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.badRequest().body("发送配送失败");
        }
        return ResponseEntity.ok().build();
    }


    @ApiOperation(value = "修改订单状态(DISPATCHING,RECEIVED,其它状态请使用特定接口)", response = ResponseEntity.class)
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = ApiParamType.PATH, name = "orderCode", value = "订单Code", required = true, dataType = "String"),
            @ApiImplicitParam(paramType = ApiParamType.QUERY, name = "orderStatus", value = "修改后订单状态", required = true, dataTypeClass = OrderStatus.class)
    })
    @PutMapping("/{orderCode}/status")
    public ResponseEntity updateOrderStatus(@PathVariable("orderCode") String orderCode, @RequestParam("orderStatus") OrderStatus orderStatus) {
        OrderDetailResult orderDetailResult = orderService.findByCode(orderCode);
        if (Objects.isNull(orderDetailResult)) {
            return ResponseEntity.badRequest().body("订单不存在");
        }
        Tips tips = orderService.updateStatus(orderCode, orderDetailResult.getStatus(), orderStatus);
        if (tips.err()) {
            return ResponseEntity.badRequest().body(tips.getMessage());
        }
        publisher.publishEvent(new OrderFlowEvent(orderDetailResult.getStatus(), orderStatus, orderDetailResult.getId()));
        return ResponseEntity.ok().build();
    }

    @ApiOperation(value = "海鼎订单调货", response = ResponseEntity.class)
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "orderCode", value = "调货订单编码", dataType = "String", required = true),
            @ApiImplicitParam(paramType = "query", name = "storeId", value = "调货目标门店id", dataType = "Long", required = true),
            @ApiImplicitParam(paramType = "query", name = "operationUser", value = "操作人", dataType = "String", required = true)
    })
    @PutMapping("/{orderCode}/store")
    public ResponseEntity modifyStoreInOrder(@PathVariable("orderCode") String orderCode, @RequestParam Long storeId, @RequestParam String operationUser) {
        OrderDetailResult order = orderService.findByCode(orderCode);
        if (Objects.isNull(order)) {
            return ResponseEntity.badRequest().body("订单不存在！");
        }
//        if (!Objects.equals(WAIT_SEND_OUT, order.getStatus()) || !Objects.equals(HdStatus.SEND_OUT, order.getHdStatus())) {
//            log.info("订单状态：" + order.getStatus() + "----海鼎状态：" + order.getHdStatus());
//            return ResponseEntity.badRequest().body("当前订单状态不可调货！");
//        }
        //远程查找调货门店 不需要查询门店位置
        ResponseEntity response = baseServiceFeign.findStoreById(storeId);
        if (response.getStatusCode().isError()) {
            log.info("远程查找调货门店查询失败：{}", storeId);
            return ResponseEntity.badRequest().body("远程查找调货门店查询失败，请重试！");
        }
        ResponseEntity hdResponse = haiDingService.hdCancel(order.getHdOrderCode(), "海鼎调货");
        if (Objects.isNull(hdResponse) || !Objects.equals(HD_CANCEL_ORDER_SUCCESS_RESULT_STRING, hdResponse.getBody())) {
            log.info("海鼎取消订单编号为：" + order.getHdOrderCode());
            return ResponseEntity.badRequest().body("海鼎取消订单失败，请重试！");
        }
        Store storeInfo = Objects.requireNonNull((Store) response.getBody());
        //重新生成海鼎订单编号
        String newHdOrderCode = generator.get(0, ApplicationType.ref(order.getApplicationType()));
        //发送海鼎
        Tips tips = orderService.hdReduce(order, storeInfo, newHdOrderCode);
        if (tips.err()) {
            probeEventPublisher.publish(ProbeEvent.of(Sniffer.Type.METHOD_EXCEPTION, "订单：" + orderCode + "调货重新发送海鼎失败！"));
        }
        //修改订单hdCode以及添加调货门店信息
        try {
            orderService.changeStore(storeInfo, operationUser, order.getId(), newHdOrderCode);
        } catch (Exception e) {
            probeEventPublisher.publish(ProbeEvent.of(Sniffer.Type.METHOD_EXCEPTION, "订单：" + orderCode + "调货重新发送海鼎成功，记录日志错误！"));
        }
        return ResponseEntity.ok().body("调货成功");
    }

    @ApiOperation(value = "根据订单code查询订单详情", response = OrderDetailResult.class)
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
            return ResponseEntity.badRequest().body("获取订单失败");
        }
        return ResponseEntity.ok(order);
    }

    @ApiOperation("根据用户Id获取订单列表")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = ApiParamType.PATH, name = "userId", value = "业务用户Id", dataType = "Long", required = true),
            @ApiImplicitParam(paramType = ApiParamType.QUERY, name = "orderType", value = "订单类型", dataType = "String"),
            @ApiImplicitParam(paramType = ApiParamType.QUERY, name = "orderStatus", value = "订单状态", dataTypeClass = OrderStatus.class)
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity ordersByUserId(
            @PathVariable("userId") Long userId,
            @RequestParam(value = "orderType", required = false) String orderType,
            @RequestParam(value = "orderStatus", required = false) OrderStatus orderStatus) {
        //XXX 是否分页？
        List<OrderDetailResult> results = baseOrderMapper.selectListByUserIdAndParam(Maps.of("userId", userId, "orderType", orderType, "orderStatus", orderStatus != null ? orderStatus.toString() : null));
        return ResponseEntity.ok(CollectionUtils.isEmpty(results) ? Tuple.of(new ArrayList<>()) : Tuple.of(results));
    }
}
