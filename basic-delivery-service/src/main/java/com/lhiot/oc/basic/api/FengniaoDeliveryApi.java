package com.lhiot.oc.basic.api;

import com.leon.microx.support.result.Tips;
import com.leon.microx.util.Calculator;
import com.leon.microx.util.IOUtils;
import com.leon.microx.util.StringUtils;
import com.lhiot.oc.basic.domain.DeliverBaseOrder;
import com.lhiot.oc.basic.domain.enums.DeliverNeedConver;
import com.lhiot.oc.basic.feign.BaseDataServiceFeign;
import com.lhiot.oc.basic.feign.domain.Store;
import com.lhiot.oc.basic.service.DeliveryNoteService;
import com.lhiot.oc.basic.service.FengniaoDeliveryService;
import com.lhiot.oc.basic.util.Distance;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Slf4j
@RestController
@Api(description ="蜂鸟配送api")
@RequestMapping("/fengniao-delivery")
public class FengniaoDeliveryApi {

    private final FengniaoDeliveryService fengniaoDeliveryService;

    private final BaseDataServiceFeign baseDataServiceFeign;

    @Autowired
    public FengniaoDeliveryApi(FengniaoDeliveryService fengniaoDeliveryService, BaseDataServiceFeign baseDataServiceFeign) {
        this.baseDataServiceFeign = baseDataServiceFeign;
        this.fengniaoDeliveryService = fengniaoDeliveryService;
    }
    /**
     * 蜂鸟配送回调
     */
    @PostMapping("/callback")
    @ApiOperation(value = "蜂鸟回调", response = String.class)
    public ResponseEntity<?> fnCallBack(HttpServletRequest request){
        log.info("蜂鸟配送回调");
        try {
            InputStream result = request.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(result, "UTF-8"));
            List<String> list = IOUtils.readLines(in);
            IOUtils.closeQuietly(result);
            String resultStr = StringUtils.join("",list );
            log.info("callbackOrder-jsonData:" + resultStr);
            log.info("传入JSON字符串：" + resultStr);
            if(StringUtils.isNotBlank(resultStr)){
                resultStr = resultStr.substring(1,resultStr.length()-1);
            }
            Tips tips = fengniaoDeliveryService.callBack(resultStr);
            return Objects.equals(tips.getCode(),"-1")?ResponseEntity.badRequest().body(tips.getMessage()):ResponseEntity.ok(tips.getMessage());
        } catch (IOException e) {
            log.error("message {} " , e.getMessage());
            return ResponseEntity.badRequest().body("配送回调处理失败,"+e.getMessage());
        }
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
        Tips cancelResult = fengniaoDeliveryService.cancel(hdOrderCode,cancelReasonId,cancelReason);

        if(Objects.equals(cancelResult.getCode(),"1")){
            return ResponseEntity.ok(cancelResult);
        }
        return ResponseEntity.badRequest().body(cancelResult);
    }

    @ApiOperation(value = "发送蜂鸟配送单")
    @PostMapping("/send/{deliverNeedConver}")
    public ResponseEntity<Tips> send(@PathVariable("deliverNeedConver") DeliverNeedConver deliverNeedConver, @RequestBody DeliverBaseOrder deliverBaseOrder){
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
        //发送蜂鸟配送
        return ResponseEntity.ok(fengniaoDeliveryService.send(deliverNeedConver,deliverBaseOrder));
    }

    @GetMapping("/detail/{hdOrderCode}")
    @ApiOperation(value = "蜂鸟配送第三方详细信息", response = String.class)
    public ResponseEntity<String> detail(@PathVariable("hdOrderCode") String hdOrderCode) {
        return ResponseEntity.ok(fengniaoDeliveryService.detail(hdOrderCode));
    }

}
