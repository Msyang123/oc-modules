package com.lhiot.oc.delivery.api;

import com.leon.microx.support.result.Tips;
import com.leon.microx.util.Calculator;
import com.leon.microx.util.StringUtils;
import com.lhiot.oc.delivery.domain.DeliverBaseOrder;
import com.lhiot.oc.delivery.domain.enums.CoordinateSystem;
import com.lhiot.oc.delivery.feign.BasicDataService;
import com.lhiot.oc.delivery.feign.domain.Store;
import com.lhiot.oc.delivery.service.FengniaoDeliveryService;
import com.lhiot.oc.delivery.util.Distance;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Objects;

@Slf4j
@RestController
@Api(description = "蜂鸟配送api")
@RequestMapping("/fengniao-delivery")
public class FengNiaoDeliveryApi {

    private final FengniaoDeliveryService fengniaoDeliveryService;

    private final BasicDataService basicDataService;

    @Autowired
    public FengNiaoDeliveryApi(FengniaoDeliveryService fengniaoDeliveryService, BasicDataService basicDataService) {
        this.basicDataService = basicDataService;
        this.fengniaoDeliveryService = fengniaoDeliveryService;
    }

    /**
     * 蜂鸟配送回调
     */
    @PostMapping("/callback")
    @ApiOperation(value = "蜂鸟回调", response = String.class)
    public ResponseEntity<?> fnCallback(@RequestBody String backMsg) {
        log.info("蜂鸟配送回调");
        log.info("callbackOrder-jsonData:" + backMsg);
        if (StringUtils.isNotBlank(backMsg)) {
            backMsg = backMsg.substring(1, backMsg.length() - 1);
        }
        Tips tips = fengniaoDeliveryService.callback(backMsg);
        return Objects.equals(tips.getCode(), "-1") ? ResponseEntity.badRequest().body(tips.getMessage()) : ResponseEntity.ok(tips.getMessage());
    }


    @GetMapping("/cancel/reasons")
    @ApiOperation(value = "蜂鸟配送取消原因列表", response = String.class)
    public ResponseEntity<String> cancelReasons() {
        return ResponseEntity.ok(fengniaoDeliveryService.cancelOrderReasons());
    }

    @PostMapping("/cancel")
    @ApiOperation(value = "蜂鸟配送取消订单", response = String.class)
    public ResponseEntity<Tips> cancel(@RequestParam("hdOrderCode") String hdOrderCode,
                                       @RequestParam("cancelReasonId") Integer cancelReasonId,
                                       @RequestParam("cancelReason") String cancelReason) {

        //向第三方取消配送
        Tips cancelResult = fengniaoDeliveryService.cancel(hdOrderCode, cancelReasonId, cancelReason);

        if (Objects.equals(cancelResult.getCode(), "1")) {
            return ResponseEntity.ok(cancelResult);
        }
        return ResponseEntity.badRequest().body(cancelResult);
    }

    @ApiOperation(value = "发送蜂鸟配送单")
    @PostMapping("/send/{coordinateSystem}")
    public ResponseEntity<Tips> send(@PathVariable("coordinateSystem") CoordinateSystem coordinateSystem, @RequestBody DeliverBaseOrder deliverBaseOrder) {
        //查询送货门店
        ResponseEntity response = basicDataService.findStoreByCode(deliverBaseOrder.getStoreCode(), deliverBaseOrder.getApplyType());
        if (response.getStatusCode().isError() || Objects.isNull(response.getBody())) {
            return ResponseEntity.badRequest().body(Tips.warn("查询门店信息失败"));
        }
        Store store = (Store) response.getBody();
        //距离换算
        BigDecimal distance = Distance.getDistance(
                store.getStorePosition().getLat(), store.getStorePosition().getLng(), deliverBaseOrder.getLat(), deliverBaseOrder.getLng()
        );
        if (Calculator.gt(distance.doubleValue(), 5.00)) {
            log.error("超过配送范围！{}", distance);
            return ResponseEntity.badRequest().body(Tips.of(-1, "超过配送范围！"));
        }
        //发送蜂鸟配送
        return ResponseEntity.ok(fengniaoDeliveryService.send(coordinateSystem, deliverBaseOrder));
    }

    @GetMapping("/detail/{hdOrderCode}")
    @ApiOperation(value = "蜂鸟配送第三方详细信息", response = String.class)
    public ResponseEntity<String> detail(@PathVariable("hdOrderCode") String hdOrderCode) {
        return ResponseEntity.ok(fengniaoDeliveryService.detail(hdOrderCode));
    }

}
