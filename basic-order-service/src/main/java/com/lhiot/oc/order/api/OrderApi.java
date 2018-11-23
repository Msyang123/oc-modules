package com.lhiot.oc.order.api;

import com.leon.microx.probe.annotation.Sniffer;
import com.leon.microx.probe.collector.ProbeEventPublisher;
import com.leon.microx.probe.event.ProbeEvent;
import com.leon.microx.util.*;
import com.leon.microx.web.result.Multiple;
import com.leon.microx.web.result.Tips;
import com.leon.microx.web.swagger.ApiParamType;
import com.lhiot.oc.order.entity.type.HdStatus;
import com.lhiot.oc.order.entity.type.OrderRefundStatus;
import com.lhiot.oc.order.entity.type.OrderStatus;
import com.lhiot.oc.order.event.OrderFlowEvent;
import com.lhiot.oc.order.feign.BaseServiceFeign;
import com.lhiot.oc.order.feign.HaiDingService;
import com.lhiot.oc.order.mapper.BaseOrderMapper;
import com.lhiot.oc.order.model.*;
import com.lhiot.oc.order.service.OrderService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.lhiot.oc.order.entity.type.OrderStatus.WAIT_SEND_OUT;

/**
 * zhangfeng created in 2018/9/19 9:42
 **/
@RestController
@Slf4j
@RequestMapping("/orders")
@Transactional
public class OrderApi {

    private OrderService orderService;
    private BaseOrderMapper baseOrderMapper;
    private RedissonClient redissonClient;
    private BaseServiceFeign baseServiceFeign;
    private ApplicationEventPublisher publisher;
    private ProbeEventPublisher probeEventPublisher;
    private HaiDingService haiDingService;
    private SnowflakeId snowflakeId;
    private static final String HD_CANCEL_ORDER_SUCCESS_RESULT_STRING = "{\"success\":true}";

    public OrderApi(OrderService orderService, BaseOrderMapper baseOrderMapper, RedissonClient redissonClient, BaseServiceFeign baseServiceFeign, ApplicationEventPublisher publisher, ProbeEventPublisher probeEventPublisher, HaiDingService haiDingService, SnowflakeId snowflakeId) {
        this.orderService = orderService;
        this.baseOrderMapper = baseOrderMapper;
        this.redissonClient = redissonClient;
        this.baseServiceFeign = baseServiceFeign;
        this.publisher = publisher;
        this.probeEventPublisher = probeEventPublisher;
        this.haiDingService = haiDingService;
        this.snowflakeId = snowflakeId;
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
            @ApiImplicitParam(paramType = ApiParamType.QUERY, name = "orderType", value = "订单类型", dataType = "String")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity ordersByUserId(@PathVariable("userId") Long userId, @RequestParam(value = "orderType", required = false) String orderType) {
        List<OrderDetailResult> results = baseOrderMapper.selectListByUserIdAndOrderType(Maps.of("userId", userId, "orderType", orderType));
        return ResponseEntity.ok(CollectionUtils.isEmpty(results) ? Multiple.of(new ArrayList<>()) : Multiple.of(results));
    }

    @ApiOperation(value = "修改订单状态", response = ResponseEntity.class)
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = ApiParamType.PATH, name = "orderCode", value = "订单Code", required = true, dataType = "String"),
            @ApiImplicitParam(paramType = ApiParamType.QUERY, name = "orderStatus", value = "修改后订单状态", required = true, dataType = "OrderStatus")
    })
    @PutMapping("/{orderCode}/status")
    public ResponseEntity cancelOrder(@PathVariable("orderCode") String orderCode, @RequestParam("orderStatus") OrderStatus orderStatus) {
        OrderDetailResult orderDetailResult = orderService.findByCode(orderCode);
        if (Objects.isNull(orderDetailResult)) {
            return ResponseEntity.badRequest().body("订单不存在！");
        }
        Tips tips = orderService.updateStatus(orderDetailResult, orderStatus);
        if (tips.err()) {
            return ResponseEntity.badRequest().body(tips.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    @ApiOperation(value = "订单退货(包括部分和全部)", response = ResponseEntity.class)
    @ApiImplicitParam(paramType = "path", name = "orderCode", value = "订单orderCode", required = true, dataType = "String")
    @PutMapping("/{orderCode}/refund")
    public ResponseEntity refundOrder(@PathVariable("orderCode") String orderCode, @NotNull @RequestBody ReturnOrderParam returnOrderParam) {
        //防止一个订单重复提交
        RBucket<String> rBucket = redissonClient.getBucket("order_refund_" + orderCode);
        String rBucketId = rBucket.get();
        if (StringUtils.isNotBlank(rBucketId) && rBucketId.equals(orderCode)) {
            return ResponseEntity.badRequest().body("退货中。。。。");
        }
        rBucket.set(orderCode, 30, TimeUnit.SECONDS);

        Tips tips = orderService.validateRefund(orderCode, returnOrderParam);
        if (tips.err()) {
            return ResponseEntity.badRequest().body(tips.getMessage());
        }
        OrderDetailResult order = (OrderDetailResult) tips.getData();
        Pair<OrderRefundStatus, OrderStatus> pair = null;
        switch (order.getStatus()) {
            case WAIT_SEND_OUT:
                pair = orderService.hdCancel(order.getHdOrderCode(), returnOrderParam.getReason());
                break;
            case SEND_OUT:
            case RECEIVED:
                pair = orderService.hdRefund(order, returnOrderParam);
                break;
            default:
                return ResponseEntity.badRequest().body("海鼎退货失败！");
        }
        if (pair.isEmpty()) {
            return ResponseEntity.badRequest().body("海鼎退货失败！");
        }
        try {
            orderService.refundUpdateByCode(pair, returnOrderParam, order);
        } catch (Exception e) {
            probeEventPublisher.publish(
                    ProbeEvent.of(Sniffer.Type.API_EXCEPTION, "记录退款日志错误")
                            .stackTrace(Exceptions.stackTrace(e))
            );
        }
        //构建写order_flow库的数据
        this.publisher.publishEvent(
                new OrderFlowEvent(order.getStatus(), pair.getSecond(), order.getId())
        );
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
        if (!Objects.equals(WAIT_SEND_OUT, order.getStatus()) || !Objects.equals(HdStatus.SEND_OUT, order.getHdStatus())) {
            log.info("订单状态：" + order.getStatus() + "----海鼎状态：" + order.getHdStatus());
            return ResponseEntity.badRequest().body("当前订单状态不可调货！");
        }
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
        String newHdOrderCode = snowflakeId.stringId();
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
}
