package com.lhiot.oc.delivery.api;

import com.leon.microx.support.result.Tips;
import com.leon.microx.util.Calculator;
import com.lhiot.oc.delivery.dada.DadaDeliveryClient;
import com.lhiot.oc.delivery.domain.DeliverBaseOrder;
import com.lhiot.oc.delivery.domain.enums.CoordinateSystem;
import com.lhiot.oc.delivery.feign.BasicDataService;
import com.lhiot.oc.delivery.feign.domain.Store;
import com.lhiot.oc.delivery.service.DadaDeliveryService;
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
@Api(description = "达达配送api")
@RequestMapping("/dada-delivery")
public class DadaDeliveryApi {

    private final DadaDeliveryService dadaDeliveryService;

    private final DadaDeliveryClient dadaClient;

    private final BasicDataService basicDataService;

    @Autowired
    public DadaDeliveryApi(DadaDeliveryService dadaDeliveryService, DadaDeliveryClient dadaClient, BasicDataService basicDataService) {

        this.dadaDeliveryService = dadaDeliveryService;
        this.dadaClient = dadaClient;
        this.basicDataService = basicDataService;
    }

    @PostMapping("/callback")
    @ApiOperation(value = "达达配送回调处理api(业务接口调用)", response = String.class)
    public ResponseEntity<String> callBack(@RequestBody String backMsg) {
        log.info("backOrder-jsonData:" + backMsg);

        Tips tips = dadaDeliveryService.callback(backMsg);
        return Objects.equals(tips.getCode(), "-1") ? ResponseEntity.badRequest().body(tips.getMessage()) : ResponseEntity.ok(tips.getMessage());
    }

    @GetMapping("/cancel/reasons")
    @ApiOperation(value = "达达配送取消原因列表", response = String.class)
    public ResponseEntity<String> cancelReasons() {
        return ResponseEntity.ok(dadaDeliveryService.cancelOrderReasons());
    }

    @PostMapping("/cancel")
    @ApiOperation(value = "达达配送取消订单", response = String.class)
    public ResponseEntity<Tips> cancel(@RequestParam("hdOrderCode") String hdOrderCode,
                                       @RequestParam("cancelReasonId") Integer cancelReasonId,
                                       @RequestParam("cancelReason") String cancelReason) {

        //向第三方取消配送
        Tips cancelResult = dadaDeliveryService.cancel(hdOrderCode, cancelReasonId, cancelReason);

        if (Objects.equals(cancelResult.getCode(), "1")) {
            return ResponseEntity.ok(cancelResult);
        }
        return ResponseEntity.badRequest().body(cancelResult);
    }

    @ApiOperation(value = "发送达达配送单")
    @PostMapping("/send/{coordinateSystem}")
    public ResponseEntity<Tips> sendToDada(@PathVariable("coordinateSystem") CoordinateSystem coordinateSystem, @RequestBody DeliverBaseOrder deliverBaseOrder) {
        //查询送货门店
        ResponseEntity response = basicDataService.findStoreByCode(deliverBaseOrder.getStoreCode(), deliverBaseOrder.getApplyType());
        if (response.getStatusCode().isError() || Objects.isNull(response.getBody())) {
            return ResponseEntity.badRequest().body(Tips.warn("查询门店信息失败"));
        }
        Store store = (Store) response.getBody();
        //距离换算
        BigDecimal distance = Distance.getDistance(store.getStorePosition().getLat(), store.getStorePosition().getLng(),
                deliverBaseOrder.getLat(), deliverBaseOrder.getLng());
        if (Calculator.gt(distance.doubleValue(), 5.00)) {
            log.error("超过配送范围！{}", distance);
            return ResponseEntity.badRequest().body(Tips.of(-1, "超过配送范围！"));
        }
        //发送达达
        return ResponseEntity.ok(dadaDeliveryService.send(coordinateSystem, deliverBaseOrder));
    }

    @GetMapping("/detail/{hdOrderCode}")
    @ApiOperation(value = "达达配送第三方详细信息", response = String.class)
    public ResponseEntity<String> detail(@PathVariable("hdOrderCode") String hdOrderCode) {
        return ResponseEntity.ok(dadaDeliveryService.detail(hdOrderCode));
    }


    @ApiOperation(value = "达达模拟配送接单")
    @GetMapping("/accept/{hdOrderCode}")
    public ResponseEntity<String> accept(@PathVariable("hdOrderCode") String hdOrderCode) {
        return ResponseEntity.ok(dadaClient.accept(hdOrderCode));
    }

    @ApiOperation(value = "达达模拟配送取货")
    @GetMapping("/fetch/{hdOrderCode}")
    public ResponseEntity<String> fetch(@PathVariable("hdOrderCode") String hdOrderCode) {
        return ResponseEntity.ok(dadaClient.fetch(hdOrderCode));
    }

    @ApiOperation(value = "达达模拟配送完成")
    @GetMapping("/finish/{hdOrderCode}")
    public ResponseEntity<String> finish(@PathVariable("hdOrderCode") String hdOrderCode) {
        return ResponseEntity.ok(dadaClient.finish(hdOrderCode));
    }

    @ApiOperation(value = "达达模拟配送取消")
    @GetMapping("/cancel/{hdOrderCode}")
    public ResponseEntity<String> cancel(@PathVariable("hdOrderCode") String hdOrderCode) {
        return ResponseEntity.ok(dadaClient.cancel(hdOrderCode));
    }

    @ApiOperation(value = "达达模拟配送过期")
    @GetMapping("/expire/{hdOrderCode}")
    public ResponseEntity<String> expire(@PathVariable("hdOrderCode") String hdOrderCode) {
        return ResponseEntity.ok(dadaClient.expire(hdOrderCode));
    }


}
