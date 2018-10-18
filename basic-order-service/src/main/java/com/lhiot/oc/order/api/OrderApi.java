package com.lhiot.oc.order.api;

import com.leon.microx.util.BeanUtils;
import com.leon.microx.util.StringUtils;
import com.leon.microx.web.result.Tips;
import com.lhiot.oc.order.event.OrderFlowEvent;
import com.lhiot.oc.order.feign.BaseServiceFeign;
import com.lhiot.oc.order.feign.HaiDingService;
import com.lhiot.oc.order.model.*;
import com.lhiot.oc.order.model.type.AllowRefund;
import com.lhiot.oc.order.model.type.OrderRefundStatus;
import com.lhiot.oc.order.model.type.OrderStatus;
import com.lhiot.oc.order.service.OrderProductService;
import com.lhiot.oc.order.service.OrderService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.lhiot.oc.order.model.type.OrderStatus.WAIT_SEND_OUT;

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
    private HaiDingService haiDingService;
    private OrderProductService orderProductService;
    private static final String HD_CANCEL_ORDER_SUCCESS_RESULT_STRING = "{\"success\":true}";

    public OrderApi(OrderService orderService, BaseServiceFeign baseServiceFeign, ApplicationEventPublisher publisher, HaiDingService haiDingService, OrderProductService orderProductService) {
        this.orderService = orderService;
        this.baseServiceFeign = baseServiceFeign;
        this.publisher = publisher;
        this.haiDingService = haiDingService;
        this.orderProductService = orderProductService;
    }

    @PostMapping("/")
    @ApiOperation(value = "创建订单", response = OrderDetailResult.class)
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
            return ResponseEntity.badRequest().body(Tips.of(-1, "获取订单失败"));
        }
        return ResponseEntity.ok(order);
    }

    @ApiOperation(value = "取消订单", response = ResponseEntity.class)
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

    @ApiOperation(value = "订单退货(包括部分和全部)", response = ResponseEntity.class)
    @ApiImplicitParam(paramType = "path", name = "orderCode", value = "订单orderCode", required = true, dataType = "String")
    @PutMapping("/{orderCode}/refund")
    public ResponseEntity refundOrder(@PathVariable("orderCode") String orderCode, @NotNull @RequestBody ReturnOrderParam returnOrderParam) {
        OrderDetailResult searchBaseOrderInfo = orderService.findByCode(orderCode);
        if (Objects.isNull(searchBaseOrderInfo)) {
            return ResponseEntity.badRequest().body("未找到订单");
        }
        if (Objects.equals(searchBaseOrderInfo.getAllowRefund(), AllowRefund.NO)) {
            return ResponseEntity.badRequest().body("订单为非允许退货订单");
        }
        //只允许待发货 已发货 退货中的订单退货
        if (!Objects.equals(searchBaseOrderInfo.getStatus(), WAIT_SEND_OUT) &&
                !Objects.equals(searchBaseOrderInfo.getStatus(), OrderStatus.SEND_OUT) &&
                !Objects.equals(searchBaseOrderInfo.getStatus(), OrderStatus.RECEIVED)) {
            return ResponseEntity.badRequest().body("只允许待发货/已发货的订单退货，当前订单状态为:" + searchBaseOrderInfo.getStatus().getDescription());
        }
        BaseOrder baseOrder = new BaseOrder();
        OrderRefund orderRefund = new OrderRefund();
        switch (searchBaseOrderInfo.getStatus()) {
            case WAIT_SEND_OUT:
                ResponseEntity cancelResponse = haiDingService.hdCancel(searchBaseOrderInfo.getHdOrderCode(), returnOrderParam.getReason());
                if (Objects.isNull(cancelResponse) || cancelResponse.getStatusCode().isError()) {
                    return ResponseEntity.badRequest().body("取消海鼎订单失败!");
                }
                orderRefund.setOrderRefundStatus(OrderRefundStatus.ALREADY_RETURN);
                baseOrder.setStatus(OrderStatus.ALREADY_RETURN);
                break;
            case SEND_OUT:
            case RECEIVED:
                HaiDingOrderParam haiDingOrderParam = new HaiDingOrderParam();
                BeanUtils.of(haiDingOrderParam).populate(searchBaseOrderInfo);
                List<OrderProduct> refundProducts = orderProductService.findOrderProductListByIds(Arrays.asList(StringUtils.tokenizeToStringArray(returnOrderParam.getOrderProductIds(), ",")));
                if (CollectionUtils.isEmpty(refundProducts)) {
                    return ResponseEntity.badRequest().body("退货商品列表为空！");
                }
                OrderStore store = searchBaseOrderInfo.getOrderStore();
                haiDingOrderParam.setStoreName(store.getStoreName());
                haiDingOrderParam.setStoreCode(store.getStoreCode());
                haiDingOrderParam.setStoreId(store.getStoreId());
                haiDingOrderParam.setOrderProducts(refundProducts);
                ResponseEntity refundResponse = haiDingService.hdRefund(haiDingOrderParam);
                if (Objects.isNull(refundResponse) || refundResponse.getStatusCode().isError()) {
                    return ResponseEntity.badRequest().body("海鼎退货失败！");
                }
                orderRefund.setOrderRefundStatus(OrderRefundStatus.RETURNING);
                baseOrder.setStatus(OrderStatus.RETURNING);
                break;
            default:
                return ResponseEntity.badRequest().body("海鼎退货失败！");
        }

        baseOrder.setCode(searchBaseOrderInfo.getCode());
        baseOrder.setId(searchBaseOrderInfo.getId());

        BeanUtils.of(orderRefund).populate(returnOrderParam);
        orderRefund.setHdOrderCode(searchBaseOrderInfo.getHdOrderCode());
        orderRefund.setOrderId(searchBaseOrderInfo.getId());
        orderRefund.setUserId(searchBaseOrderInfo.getUserId());
        orderService.refundOrderByCode(baseOrder, orderRefund);
        //构建写order_flow库的数据
        this.publisher.publishEvent(
                new OrderFlowEvent(searchBaseOrderInfo.getStatus(), baseOrder.getStatus(), searchBaseOrderInfo.getId())
        );
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{orderCode}/dispatching")
    @ApiOperation(value = "修改订单为配送中", response = ResponseEntity.class)
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

    @ApiOperation(value = "修改订单为已收货", response = ResponseEntity.class)
    @PutMapping(value = "/{orderCode}/received")
    public ResponseEntity received(@PathVariable("orderCode") String orderCode) {
        OrderDetailResult orderDetailResult = orderService.findByCode(orderCode);
        if (Objects.isNull(orderDetailResult)) {
            return ResponseEntity.badRequest().body("订单不存在");
        }
        if (!Objects.equals(orderDetailResult.getStatus(), WAIT_SEND_OUT) && !Objects.equals(orderDetailResult.getStatus(), OrderStatus.DISPATCHING)) {
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


    @ApiOperation(value = "海鼎订单调货", response = ResponseEntity.class)
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
        if (!Objects.equals(WAIT_SEND_OUT, searchBaseOrderInfo.getStatus()) || !Objects.equals(HdStatus.SEND_OUT, searchBaseOrderInfo.getHdStatus())) {
            log.info("订单状态：" + searchBaseOrderInfo.getStatus() + "----海鼎状态：" + searchBaseOrderInfo.getHdStatus());
            return ResponseEntity.badRequest().body("当前订单状态不可调货！");
        }
        //远程查找调货门店 不需要查询门店位置
        ResponseEntity storeInfoResponseEntity = baseServiceFeign.findStoreById(storeId, null);
        if (storeInfoResponseEntity == null || storeInfoResponseEntity.getStatusCode().isError()) {
            log.info("远程查找调货门店查询失败：{}", storeId);
            return ResponseEntity.badRequest().body("远程查找调货门店查询失败，请重试！");
        }
        Store storeInfo = (Store) storeInfoResponseEntity.getBody();
        if (storeInfo == null) {
            log.info("远程查找调货门店查询未找到门店：{}", storeId);
            return ResponseEntity.badRequest().body("远程查找调货门店查询未找到门店，请重试！");
        }

        ResponseEntity hdResponse = haiDingService.hdCancel(searchBaseOrderInfo.getHdOrderCode(), "海鼎调货");
        if (Objects.isNull(hdResponse) || !Objects.equals(HD_CANCEL_ORDER_SUCCESS_RESULT_STRING, hdResponse.getBody())) {
            log.info("海鼎取消订单编号为：" + searchBaseOrderInfo.getHdOrderCode());
            return ResponseEntity.badRequest().body("海鼎取消订单失败，请重试！");
        }

        HaiDingOrderParam haiDingOrderParam = new HaiDingOrderParam();
        BeanUtils.of(haiDingOrderParam).populate(searchBaseOrderInfo);
        haiDingOrderParam.setStoreName(storeInfo.getName());
        haiDingOrderParam.setStoreCode(storeInfo.getCode());
        haiDingOrderParam.setStoreId(storeInfo.getId());
        //TODO 发送海鼎新的门店订单信息 是否需要校验库存 待定
        ResponseEntity hdReduceResponse = haiDingService.reduce(haiDingOrderParam); //thirdPartyServiceFeign.hdReduce(orderInfo);
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
