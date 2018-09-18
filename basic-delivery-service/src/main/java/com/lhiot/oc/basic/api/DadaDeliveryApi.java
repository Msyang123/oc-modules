package com.lhiot.oc.basic.api;

import com.leon.microx.support.result.Tips;
import com.leon.microx.util.Calculator;
import com.lhiot.oc.basic.domain.DeliverBaseOrder;
import com.lhiot.oc.basic.domain.enums.DeliverNeedConver;
import com.lhiot.oc.basic.feign.BaseDataServiceFeign;
import com.lhiot.oc.basic.feign.domain.Store;
import com.lhiot.oc.basic.service.DadaDeliveryService;
import com.lhiot.oc.basic.util.Distance;
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
@Api(description ="达达配送api")
@RequestMapping("/dada-delivery")
public class DadaDeliveryApi {

    private final DadaDeliveryService dadaDeliveryService;

    private final BaseDataServiceFeign baseDataServiceFeign;

    @Autowired
    public DadaDeliveryApi(DadaDeliveryService dadaDeliveryService, BaseDataServiceFeign baseDataServiceFeign) {

        this.dadaDeliveryService = dadaDeliveryService;
        this.baseDataServiceFeign = baseDataServiceFeign;
    }
    /**
     * 达达配送回调处理api(业务接口调用)
     *
     * @param backMsg
     */
    @PostMapping("/callback")
    @ApiOperation(value = "达达配送回调处理api(业务接口调用)", response = String.class)
    public ResponseEntity<String> callBack(@RequestBody String backMsg) {
        log.info("backOrder-jsonData:" + backMsg);

        Tips tips = dadaDeliveryService.callBack(backMsg);
        return Objects.equals(tips.getCode(),"-1")?ResponseEntity.badRequest().body(tips.getMessage()):ResponseEntity.ok(tips.getMessage());
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
        Tips cancelResult = dadaDeliveryService.cancel(hdOrderCode,cancelReasonId,cancelReason);

        if(Objects.equals(cancelResult.getCode(),"1")){
            return ResponseEntity.ok(cancelResult);
        }
        return ResponseEntity.badRequest().body(cancelResult);
    }

    @ApiOperation(value = "发送达达配送单")
    @PostMapping("/send/{deliverNeedConver}")
    public ResponseEntity<Tips> sendToDada(@PathVariable("deliverNeedConver") DeliverNeedConver deliverNeedConver, @RequestBody DeliverBaseOrder deliverBaseOrder) {
        //查询送货门店
        ResponseEntity<Store> storeResponseEntity = baseDataServiceFeign.findStoreByCode(deliverBaseOrder.getStoreCode(),deliverBaseOrder.getApplyType());
        if(Objects.isNull(storeResponseEntity)||!storeResponseEntity.getStatusCode().is2xxSuccessful()){
            return ResponseEntity.badRequest().body(Tips.of(-1,"查询门店信息失败"));
        }

        Store store=storeResponseEntity.getBody();
        //距离换算
        BigDecimal distance = Distance.getDistance(store.getStorePosition().getLat(),store.getStorePosition().getLng(),
                deliverBaseOrder.getLat(),deliverBaseOrder.getLng());
        if(Calculator.gt(distance.doubleValue(),5.00)){
            log.error("超过配送范围！{}",distance);
            return ResponseEntity.badRequest().body(Tips.of(-1,"超过配送范围！"));
        }
        //发送达达
        return ResponseEntity.ok(dadaDeliveryService.send(deliverNeedConver,deliverBaseOrder));
    }

    @GetMapping("/detail/{hdOrderCode}")
    @ApiOperation(value = "达达配送第三方详细信息", response = String.class)
    public ResponseEntity<String> detail(@PathVariable("hdOrderCode") String hdOrderCode) {
        return ResponseEntity.ok(dadaDeliveryService.detail(hdOrderCode));
    }

}
