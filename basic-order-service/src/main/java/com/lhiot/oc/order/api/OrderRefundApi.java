package com.lhiot.oc.order.api;

import com.leon.microx.probe.annotation.Sniffer;
import com.leon.microx.probe.collector.ProbeEventPublisher;
import com.leon.microx.probe.event.ProbeEvent;
import com.leon.microx.redisson.DistributedLock;
import com.leon.microx.util.Exceptions;
import com.leon.microx.web.result.Tips;
import com.leon.microx.web.swagger.ApiHideBodyProperty;
import com.leon.microx.web.swagger.ApiParamType;
import com.lhiot.oc.order.entity.type.OrderRefundStatus;
import com.lhiot.oc.order.mapper.BaseOrderMapper;
import com.lhiot.oc.order.model.OrderDetailResult;
import com.lhiot.oc.order.model.ReturnOrderParam;
import com.lhiot.oc.order.service.OrderRefundService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author zhangfeng create in 10:58 2018/12/6
 */
@Slf4j
@RestController
public class OrderRefundApi {
    private OrderRefundService refundService;
    private BaseOrderMapper baseOrderMapper;
    private ProbeEventPublisher probeEventPublisher;

    public OrderRefundApi(OrderRefundService refundService, BaseOrderMapper baseOrderMapper, ProbeEventPublisher probeEventPublisher) {
        this.refundService = refundService;
        this.baseOrderMapper = baseOrderMapper;
        this.probeEventPublisher = probeEventPublisher;
    }


    @ApiOperation("订单未发送海鼎，退款")
    @PutMapping("orders/{orderCode}/not-send-hd/refund")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = ApiParamType.PATH, name = "orderCode", value = "订单编号", dataType = "String", required = true),
            @ApiImplicitParam(paramType = ApiParamType.BODY, name = "param", value = "退款传入参数", dataTypeClass = ReturnOrderParam.class)
    })
    @ApiHideBodyProperty("orderProductIds")
    @DistributedLock(name = "'order-flow-lock-' + #orderCode")
    public ResponseEntity notSendHdRefund(@PathVariable("orderCode") String orderCode, @RequestBody ReturnOrderParam param) {
        Tips tips = refundService.validateRefund(orderCode, param);
        if (tips.err()) {
            return ResponseEntity.badRequest().body(tips.getMessage());
        }
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
            @ApiImplicitParam(paramType = ApiParamType.BODY, name = "param", value = "退款传入参数", dataTypeClass = ReturnOrderParam.class)
    })
    @PutMapping("orders/{orderCode}/send-hd/refund")
    @DistributedLock(name = "'order-flow-lock-' + #orderCode")
    public ResponseEntity sendHdRefund(@PathVariable("orderCode") String orderCode, @RequestBody ReturnOrderParam param) {
        Tips tips = refundService.validateRefund(orderCode, param);
        if (tips.err()) {
            return ResponseEntity.badRequest().body(tips.getMessage());
        }
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
            @ApiImplicitParam(paramType = ApiParamType.BODY, name = "param", value = "退款传入参数", dataTypeClass = ReturnOrderParam.class)
    })
    @ApiHideBodyProperty({"notifyUrl", "fee"})
    @PutMapping("orders/{orderCode}/returns")
    @DistributedLock(name = "'order-flow-lock-' + #orderCode")
    public ResponseEntity stockUpRefund(@PathVariable("orderCode") String orderCode, @RequestBody ReturnOrderParam param) {
        Tips tips = refundService.validateRefund(orderCode, param);
        if (tips.err()) {
            return ResponseEntity.badRequest().body(tips.getMessage());
        }
        try {
            refundService.applyHdReturns(orderCode, param);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.badRequest().body("提交海鼎退货申请失败");
        }
        return ResponseEntity.ok().build();
    }

    @ApiOperation("备货退货，确认收到货，进行退款")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = ApiParamType.PATH, name = "orderCode", value = "订单编号", dataType = "String", required = true),
            @ApiImplicitParam(paramType = ApiParamType.BODY, name = "param", value = "退款传入参数", dataTypeClass = ReturnOrderParam.class)
    })
    @ApiHideBodyProperty({"notifyUrl", "fee"})
    @PutMapping("orders/{orderCode}/refund")
    @DistributedLock(name = "'order-flow-lock-' + #orderCode")
    public ResponseEntity orderRefund(@PathVariable("orderCode") String orderCode, @RequestBody ReturnOrderParam param) {
        OrderDetailResult order = baseOrderMapper.selectByCode(orderCode);
        try {
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
            @ApiImplicitParam(paramType = ApiParamType.PATH, name = "orderCode", value = "订单编号", dataType = "String", required = true),
            @ApiImplicitParam(paramType = ApiParamType.QUERY, name = "refundStatus", value = "退款状态", dataTypeClass = OrderRefundStatus.class, required = true)
    })
    @PutMapping("orders/{orderCode}/refund/confirmation")
    @DistributedLock(name = "'order-flow-lock-' + #orderCode")
    public ResponseEntity confirmRefund(@PathVariable("orderCode") String orderCode, @RequestParam OrderRefundStatus refundStatus) {
        try {
            Tips tips = refundService.confirmRefund(orderCode, refundStatus);
            if (tips.err()) {
                return ResponseEntity.badRequest().body("确认退款失败");
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.badRequest().body("确认退款失败");
        }
    }

}
