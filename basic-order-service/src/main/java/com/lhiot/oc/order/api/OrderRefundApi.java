package com.lhiot.oc.order.api;

import com.leon.microx.probe.annotation.Sniffer;
import com.leon.microx.probe.collector.ProbeEventPublisher;
import com.leon.microx.probe.event.ProbeEvent;
import com.leon.microx.redisson.DistributedLock;
import com.leon.microx.util.Beans;
import com.leon.microx.util.Exceptions;
import com.leon.microx.util.Maps;
import com.leon.microx.web.result.Tips;
import com.leon.microx.web.swagger.ApiHideBodyProperty;
import com.leon.microx.web.swagger.ApiParamType;
import com.lhiot.oc.order.entity.OrderRefund;
import com.lhiot.oc.order.entity.type.OrderRefundStatus;
import com.lhiot.oc.order.entity.type.RefundType;
import com.lhiot.oc.order.mapper.BaseOrderMapper;
import com.lhiot.oc.order.mapper.OrderRefundMapper;
import com.lhiot.oc.order.model.OrderDetailResult;
import com.lhiot.oc.order.model.ReturnOrderParam;
import com.lhiot.oc.order.model.type.NotPayRefundWay;
import com.lhiot.oc.order.service.OrderRefundService;
import com.lhiot.oc.order.service.OrderService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

/**
 * @author zhangfeng create in 10:58 2018/12/6
 */
@Slf4j
@RestController
public class OrderRefundApi {
    private OrderRefundService refundService;
    private BaseOrderMapper baseOrderMapper;
    private ProbeEventPublisher probeEventPublisher;
    private OrderService orderService;
    private OrderRefundMapper refundMapper;

    public OrderRefundApi(OrderRefundService refundService, BaseOrderMapper baseOrderMapper, ProbeEventPublisher probeEventPublisher, OrderService orderService, OrderRefundMapper refundMapper) {
        this.refundService = refundService;
        this.baseOrderMapper = baseOrderMapper;
        this.probeEventPublisher = probeEventPublisher;
        this.orderService = orderService;
        this.refundMapper = refundMapper;
    }


    @ApiOperation("订单未发送海鼎，退款")
    @PutMapping("orders/{orderCode}/not-send-hd/refund")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = ApiParamType.PATH, name = "orderCode", value = "订单编号", dataType = "String", required = true),
            @ApiImplicitParam(paramType = ApiParamType.BODY, name = "param", value = "退款传入参数", dataType = "ReturnOrderParam")
    })
    @ApiHideBodyProperty("orderProductIds")
    @DistributedLock(name = "'order-flow-lock-' + #orderCode")
    public ResponseEntity notSendHdRefund(@PathVariable("orderCode") String orderCode, @RequestBody ReturnOrderParam param) {
        try {
            refundService.notSendHdRefund(orderCode, param);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.badRequest().body("退款失败");
        }

        return ResponseEntity.ok().build();
    }

    @ApiOperation("订单发送海鼎，未备货退货")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = ApiParamType.PATH, name = "orderCode", value = "订单编号", dataType = "String", required = true),
            @ApiImplicitParam(paramType = ApiParamType.BODY, name = "param", value = "退款传入参数", dataType = "ReturnOrderParam")
    })
    @PutMapping("orders/{orderCode}/send-hd/refund")
    @DistributedLock(name = "'order-flow-lock-' + #orderCode")
    public ResponseEntity sendHdRefund(@PathVariable("orderCode") String orderCode, @RequestBody ReturnOrderParam param) {
        try {
            refundService.sendHdRefund(orderCode, param);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.badRequest().body("退款失败");
        }
        return ResponseEntity.ok().build();
    }

    @ApiOperation("海鼎备货后提交退货")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = ApiParamType.PATH, name = "orderCode", value = "订单编号", dataType = "String", required = true),
            @ApiImplicitParam(paramType = ApiParamType.BODY, name = "param", value = "退款传入参数", dataType = "ReturnOrderParam")
    })
    @ApiHideBodyProperty({"notifyUrl", "fee"})
    @PutMapping("orders/{orderCode}/returns")
    @DistributedLock(name = "'order-flow-lock-' + #orderCode")
    public ResponseEntity stockUpRefund(@PathVariable("orderCode") String orderCode, @RequestBody ReturnOrderParam param) {
        try {
            OrderDetailResult order = orderService.findByCode(orderCode);
            refundService.applyHdReturns(order, param);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.badRequest().body("提交海鼎退货申请失败");
        }
        return ResponseEntity.ok().build();
    }

    @ApiOperation("备货退货，确认收到货，进行退款")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = ApiParamType.PATH, name = "orderCode", value = "订单编号", dataType = "String", required = true),
            @ApiImplicitParam(paramType = ApiParamType.BODY, name = "param", value = "退款传入参数", dataType = "ReturnOrderParam", dataTypeClass = ReturnOrderParam.class)
    })
    @PutMapping("orders/{orderCode}/refund")
    @DistributedLock(name = "'order-flow-lock-' + #orderCode")
    public ResponseEntity orderRefund(@PathVariable("orderCode") String orderCode, @RequestBody ReturnOrderParam param) {
        OrderDetailResult order = baseOrderMapper.selectByCode(orderCode);
        try {
            OrderRefund orderRefund = refundMapper.selectByOrderCode(orderCode);
            Beans.from(orderRefund).populate(param);
            refundService.refund(order.getPayId(), param);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            probeEventPublisher.publish(
                    ProbeEvent.of(Sniffer.Type.API_EXCEPTION, "记录退款日志错误")
                            .stackTrace(Exceptions.stackTrace(e))
            );
            return ResponseEntity.badRequest().body("第三方退款失败");
        }
        return ResponseEntity.ok().build();
    }

    @ApiOperation("退款确认，修改订单状态")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = ApiParamType.PATH, name = "payId", value = "订单支付Id", dataType = "String", required = true),
            @ApiImplicitParam(paramType = ApiParamType.QUERY, name = "refundStatus", value = "退款状态", dataTypeClass = OrderRefundStatus.class, required = true)
    })
    @PutMapping("orders/{payId}/refund/confirmation")
    public ResponseEntity confirmRefund(@PathVariable("payId") String payId, @RequestParam OrderRefundStatus refundStatus) {
        try {
            Tips tips = refundService.confirmRefund(payId, refundStatus);
            if (tips.err()) {
                return ResponseEntity.badRequest().body("确认退款失败");
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.badRequest().body("确认退款失败");
        }
    }

    @ApiOperation(value = "预退款接口", response = HashMap.class)
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = ApiParamType.PATH, name = "orderCode", value = "订单编号", dataType = "String", required = true),
            @ApiImplicitParam(paramType = ApiParamType.QUERY, name = "productIds", value = "退款传入参数", dataType = "String"),
            @ApiImplicitParam(paramType = ApiParamType.QUERY, name = "refundType", value = "退款类型", dataTypeClass = RefundType.class)
    })
    @GetMapping("orders/{orderCode}/refund/fee")
    public ResponseEntity fee(@PathVariable("orderCode") String orderCode, @RequestParam String productIds, @RequestParam RefundType refundType) {
        Tips tips = refundService.validateRefund(orderCode, refundType, productIds);
        if (tips.err()) {
            return ResponseEntity.badRequest().body(tips.getMessage());
        }
        OrderDetailResult order = orderService.findByCode(orderCode, true, false);
        Integer fee = refundService.fee(order, productIds);
        return ResponseEntity.ok(Maps.of("fee", fee));
    }

    @ApiOperation("订单实际未支付退货（无需退款）")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = ApiParamType.PATH, name = "orderCode", value = "订单Code", dataType = "String", required = true),
            @ApiImplicitParam(paramType = ApiParamType.QUERY, name = "notPayRefundWay", value = "无需退款的退货方式", dataTypeClass = NotPayRefundWay.class, required = true)
    })
    @PutMapping("orders/{orderCode}/not-payed/refund")
    public ResponseEntity notPayRefund(@PathVariable("orderCode") String orderCode, @RequestParam("notPayRefundWay") NotPayRefundWay refundWay) {
        Tips tips = refundService.notPayRefund(orderCode, refundWay);
        if (tips.err()) {
            return ResponseEntity.badRequest().body(tips.getMessage());
        }
        return ResponseEntity.ok().build();
    }

}
