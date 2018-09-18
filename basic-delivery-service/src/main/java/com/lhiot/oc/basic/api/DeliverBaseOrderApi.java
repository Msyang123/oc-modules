package com.lhiot.oc.basic.api;

import com.lhiot.oc.basic.domain.DeliverBaseOrder;
import com.lhiot.oc.basic.domain.common.PagerResultObject;
import com.lhiot.oc.basic.service.DeliverBaseOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
* Description:配送订单流程接口类
* @author yijun
* @date 2018/09/16
*/
@Api(description = "配送订单流程接口")
@Slf4j
@RestController
@RequestMapping("/deliverBaseOrder")
public class DeliverBaseOrderApi {

    private final DeliverBaseOrderService deliverBaseOrderService;

    @Autowired
    public DeliverBaseOrderApi(DeliverBaseOrderService deliverBaseOrderService) {
        this.deliverBaseOrderService = deliverBaseOrderService;
    }
    
    @ApiOperation(value = "根据id查询配送订单流程", notes = "根据id查询配送订单流程")
    @ApiImplicitParam(paramType = "path", name = "id", value = "主键id", required = true, dataType = "Long")
    @GetMapping("/{id}")
    public ResponseEntity<DeliverBaseOrder> findDeliverBaseOrder(@PathVariable("id") Long id) {

        return ResponseEntity.ok(deliverBaseOrderService.selectById(id));
    }
    
    @GetMapping("/page/query")
    @ApiOperation(value = "查询配送订单流程分页列表")
    public ResponseEntity<PagerResultObject<DeliverBaseOrder>> pageQuery(DeliverBaseOrder deliverBaseOrder){
        log.debug("查询配送订单流程分页列表\t param:{}",deliverBaseOrder);
        
        return ResponseEntity.ok(deliverBaseOrderService.pageList(deliverBaseOrder));
    }
    
}
