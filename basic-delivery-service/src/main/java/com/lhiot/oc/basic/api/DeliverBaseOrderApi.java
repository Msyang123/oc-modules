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

    @PostMapping("/create")
    @ApiOperation(value = "添加配送订单流程")
    @ApiImplicitParam(paramType = "body", name = "deliverBaseOrder", value = "要添加的配送订单流程", required = true, dataType = "DeliverBaseOrder")
    public ResponseEntity<Integer> create(@RequestBody DeliverBaseOrder deliverBaseOrder) {
        log.debug("添加配送订单流程\t param:{}",deliverBaseOrder);
        
        return ResponseEntity.ok(deliverBaseOrderService.create(deliverBaseOrder));
    }

    @PutMapping("/update/{id}")
    @ApiOperation(value = "根据id更新配送订单流程")
    @ApiImplicitParam(paramType = "body", name = "deliverBaseOrder", value = "要更新的配送订单流程", required = true, dataType = "DeliverBaseOrder")
    public ResponseEntity<Integer> update(@PathVariable("id") Long id,@RequestBody DeliverBaseOrder deliverBaseOrder) {
        log.debug("根据id更新配送订单流程\t id:{} param:{}",id,deliverBaseOrder);
        deliverBaseOrder.setId(id);
        
        return ResponseEntity.ok(deliverBaseOrderService.updateById(deliverBaseOrder));
    }

    @DeleteMapping("/{ids}")
    @ApiOperation(value = "根据ids删除配送订单流程")
    @ApiImplicitParam(paramType = "path", name = "ids", value = "要删除配送订单流程的ids,逗号分割", required = true, dataType = "String")
    public ResponseEntity<Integer> deleteByIds(@PathVariable("ids") String ids) {
        log.debug("根据ids删除配送订单流程\t param:{}",ids);
        
        return ResponseEntity.ok(deliverBaseOrderService.deleteByIds(ids));
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
