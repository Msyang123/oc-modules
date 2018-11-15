package com.lhiot.oc.delivery.api;

import com.leon.microx.util.Calculator;
import com.leon.microx.web.result.Tips;
import com.lhiot.oc.delivery.domain.DeliverBaseOrder;
import com.lhiot.oc.delivery.domain.DeliverNote;
import com.lhiot.oc.delivery.domain.enums.CoordinateSystem;
import com.lhiot.oc.delivery.feign.BasicDataService;
import com.lhiot.oc.delivery.feign.domain.Store;
import com.lhiot.oc.delivery.meituan.MeiTuanDeliveryClient;
import com.lhiot.oc.delivery.meituan.model.MockOrderRequest;
import com.lhiot.oc.delivery.service.DeliveryNoteService;
import com.lhiot.oc.delivery.service.MeiTuanDeliveryService;
import com.lhiot.oc.delivery.util.Distance;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Objects;

@Slf4j
@RestController
@Api(description = "美团配送api")
@RequestMapping("/mei-tuan-delivery")
public class MeiTuanDeliveryApi {

    private final MeiTuanDeliveryService meiTuanDeliveryService;

    private final MeiTuanDeliveryClient meituanClient;

    private final BasicDataService basicDataService;

    private final DeliveryNoteService deliveryNoteService;

    @Autowired
    public MeiTuanDeliveryApi(MeiTuanDeliveryService meiTuanDeliveryService, MeiTuanDeliveryClient meituanClient, BasicDataService basicDataService, DeliveryNoteService deliveryNoteService) {
        this.meiTuanDeliveryService = meiTuanDeliveryService;
        this.meituanClient = meituanClient;

        this.basicDataService = basicDataService;
        this.deliveryNoteService = deliveryNoteService;
    }

    @PostMapping("/callback")
    @ApiOperation(value = "美团配送回调处理api(业务接口调用)", response = String.class)
    public ResponseEntity<String> callBack(@RequestBody String backMsg) {
        log.info("backOrder-jsonData:" + backMsg);

        Tips tips = meiTuanDeliveryService.callback(backMsg);
        return Objects.equals(tips.getCode(), "-1") ? ResponseEntity.badRequest().body(tips.getMessage()) : ResponseEntity.ok(tips.getMessage());
    }

    @GetMapping("/cancel/reasons")
    @ApiOperation(value = "美团配送取消原因列表", response = String.class)
    public ResponseEntity<String> cancelReasons() {
        return ResponseEntity.ok(meiTuanDeliveryService.cancelOrderReasons());
    }

    @PostMapping("/cancel")
    @ApiOperation(value = "美团配送取消订单", response = String.class)
    public ResponseEntity<Tips> cancel(@RequestParam("hdOrderCode") String hdOrderCode,
                                       @RequestParam("cancelReasonId") Integer cancelReasonId,
                                       @RequestParam("cancelReason") String cancelReason) {

        //向第三方取消配送
        Tips cancelResult = meiTuanDeliveryService.cancel(hdOrderCode, cancelReasonId, cancelReason);

        if (Objects.equals(cancelResult.getCode(), "1")) {
            return ResponseEntity.ok(cancelResult);
        }
        return ResponseEntity.badRequest().body(cancelResult);
    }

    @ApiOperation(value = "发送美团配送单")
    @PostMapping("/send/{coordinateSystem}")
    public ResponseEntity<Tips> sendToMeituan(@PathVariable("coordinateSystem") CoordinateSystem coordinateSystem, @RequestBody DeliverBaseOrder deliverBaseOrder) {
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
        //发送美团
        return ResponseEntity.ok(meiTuanDeliveryService.send(coordinateSystem, deliverBaseOrder,distance));
    }

    @GetMapping("/detail/{hdOrderCode}")
    @ApiOperation(value = "美团配送第三方详细信息", response = String.class)
    public ResponseEntity<String> detail(@PathVariable("hdOrderCode") String hdOrderCode) {
        return ResponseEntity.ok(meiTuanDeliveryService.detail(hdOrderCode));
    }


    @ApiOperation(value = "美团模拟配送接单")
    @GetMapping("/accept/{hdOrderCode}")
    public ResponseEntity<String> accept(@PathVariable("hdOrderCode") String hdOrderCode) throws IOException {
        DeliverNote searchDeliverNote = deliveryNoteService.selectByDeliverCode(hdOrderCode);
        if (Objects.isNull(searchDeliverNote)) {
            return ResponseEntity.badRequest().body("未找到配送单信息");
        }
        MockOrderRequest request = new MockOrderRequest();
        request.setDeliveryId(searchDeliverNote.getId());
        request.setMtPeisongId(searchDeliverNote.getExt());
        return ResponseEntity.ok(meituanClient.accept(request));
    }

    @ApiOperation(value = "美团模拟配送取货")
    @GetMapping("/fetch/{hdOrderCode}")
    public ResponseEntity<String> fetch(@PathVariable("hdOrderCode") String hdOrderCode) throws IOException {
        DeliverNote searchDeliverNote = deliveryNoteService.selectByDeliverCode(hdOrderCode);
        if (Objects.isNull(searchDeliverNote)) {
            return ResponseEntity.badRequest().body("未找到配送单信息");
        }
        MockOrderRequest request = new MockOrderRequest();
        request.setDeliveryId(searchDeliverNote.getId());
        request.setMtPeisongId(searchDeliverNote.getExt());
        return ResponseEntity.ok(meituanClient.fetch(request));
    }

    @ApiOperation(value = "美团模拟配送完成")
    @GetMapping("/finish/{hdOrderCode}")
    public ResponseEntity<String> finish(@PathVariable("hdOrderCode") String hdOrderCode) throws IOException {
        DeliverNote searchDeliverNote = deliveryNoteService.selectByDeliverCode(hdOrderCode);
        if (Objects.isNull(searchDeliverNote)) {
            return ResponseEntity.badRequest().body("未找到配送单信息");
        }
        MockOrderRequest request = new MockOrderRequest();
        request.setDeliveryId(searchDeliverNote.getId());
        request.setMtPeisongId(searchDeliverNote.getExt());
        return ResponseEntity.ok(meituanClient.finish(request));
    }

    @ApiOperation(value = "美团模拟改变配送范围")
    @GetMapping("/rearrange/{hdOrderCode}")
    public ResponseEntity<String> rearrange(@PathVariable("hdOrderCode") String hdOrderCode) throws IOException {
        DeliverNote searchDeliverNote = deliveryNoteService.selectByDeliverCode(hdOrderCode);
        if (Objects.isNull(searchDeliverNote)) {
            return ResponseEntity.badRequest().body("未找到配送单信息");
        }
        MockOrderRequest request = new MockOrderRequest();
        request.setDeliveryId(searchDeliverNote.getId());
        request.setMtPeisongId(searchDeliverNote.getExt());
        return ResponseEntity.ok(meituanClient.rearrange(request));
    }


}
