package com.lhiot.oc.delivery.api;

import com.leon.microx.web.result.Tips;
import com.leon.microx.web.swagger.ApiParamType;
import com.lhiot.oc.delivery.client.imitaor.ImitatorDeliverType;
import com.lhiot.oc.delivery.client.imitaor.ImitatorDeliveryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 配送模拟接口
 * @author Leon (234239150@qq.com) created in 15:42 18.11.10
 */
@Slf4j
@RestController
@Api("达达配送模拟接口")
public class DeliveryImitatorApi {


    private ImitatorDeliveryService imitatorDeliveryService;

    public DeliveryImitatorApi(ImitatorDeliveryService imitatorDeliveryService) {
        this.imitatorDeliveryService = imitatorDeliveryService;
    }


    @ApiOperation("达达测试环境模拟接单")
    @ApiImplicitParam(paramType = ApiParamType.PATH,name = "hdOrderCode",value = "海鼎订单编号",required = true,dataType = "String")
    @PostMapping("acceptance/{hdOrderCode}")
    public ResponseEntity dadaAccept(@PathVariable("hdOrderCode") String hdOrderCode){
        Tips result = imitatorDeliveryService.adapt(ImitatorDeliverType.DADA).accept(hdOrderCode);
        return ResponseEntity.ok(result);
    }

    @ApiOperation("达达测试环境模拟完成配送")
    @ApiImplicitParam(paramType = ApiParamType.PATH,name = "hdOrderCode",value = "海鼎订单编号",required = true,dataType = "String")
    @PostMapping("finish/{hdOrderCode}")
    public ResponseEntity dadaFinish(@PathVariable("hdOrderCode") String hdOrderCode){
        Tips result = imitatorDeliveryService.adapt(ImitatorDeliverType.DADA).finish(hdOrderCode);
        return ResponseEntity.ok(result);
    }

    @ApiOperation("达达测试环境模拟取货")
    @ApiImplicitParam(paramType = ApiParamType.PATH,name = "hdOrderCode",value = "海鼎订单编号",required = true,dataType = "String")
    @PostMapping("fetch/{hdOrderCode}")
    public ResponseEntity dadaFetch(@PathVariable("hdOrderCode") String hdOrderCode){
        Tips result = imitatorDeliveryService.adapt(ImitatorDeliverType.DADA).fetch(hdOrderCode);
        return ResponseEntity.ok(result);
    }

    @ApiOperation("达达测试环境模拟取消")
    @ApiImplicitParam(paramType = ApiParamType.PATH,name = "hdOrderCode",value = "海鼎订单编号",required = true,dataType = "String")
    @PostMapping("expired/{hdOrderCode}")
    public ResponseEntity dadaExpire(@PathVariable("hdOrderCode") String hdOrderCode){
        Tips result = imitatorDeliveryService.adapt(ImitatorDeliverType.DADA).expire(hdOrderCode);
        return ResponseEntity.ok(result);
    }

    @ApiOperation("达达测试环境模拟过期")
    @ApiImplicitParam(paramType = ApiParamType.PATH,name = "hdOrderCode",value = "海鼎订单编号",required = true,dataType = "String")
    @PostMapping("cancel/{hdOrderCode}")
    public ResponseEntity dadaCancel(@PathVariable("hdOrderCode") String hdOrderCode){
        Tips result = imitatorDeliveryService.adapt(ImitatorDeliverType.DADA).cancel(hdOrderCode);
        return ResponseEntity.ok(result);
    }


}
