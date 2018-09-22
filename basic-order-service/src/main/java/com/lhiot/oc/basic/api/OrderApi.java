package com.lhiot.oc.basic.api;

import com.leon.microx.support.result.Tips;
import com.leon.microx.util.BeanUtils;
import com.lhiot.oc.basic.feign.BaseServiceFeign;
import com.lhiot.oc.basic.feign.domain.Store;
import com.lhiot.oc.basic.model.*;
import com.lhiot.oc.basic.service.OrderService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @Author zhangfeng created in 2018/9/19 9:42
 **/
@RestController
@Slf4j
@RequestMapping("/orders")
public class OrderApi {

    private OrderService orderService;
    private  BaseServiceFeign baseServiceFeign;

    public OrderApi(OrderService orderService, BaseServiceFeign baseServiceFeign) {
        this.orderService = orderService;
        this.baseServiceFeign = baseServiceFeign;
    }

    @PostMapping("/create/assortment")
    @ApiOperation("创建订单(套餐)--公共")
    @ApiImplicitParam(paramType = "body", name = "orderParam", dataType = "CreateOrderParam", required = true, value = "创建订单传入参数")
    @Transactional
    public ResponseEntity createOrderWithAssortment(@RequestBody CreateOrderParam orderParam) throws Exception {

        //验证参数中优惠金额及商品
        Tips backMsg = orderService.validationParam(orderParam);
        if (backMsg.getCode().equals("-1")) {
            return ResponseEntity.badRequest().body(backMsg.getMessage());
        }

        //判断门店是否存在
        ResponseEntity<Store> storeResponse = baseServiceFeign.findStoreById(orderParam.getStoreId(),orderParam.getApplicationType());
        if (Objects.isNull(storeResponse) || !storeResponse.getStatusCode().is2xxSuccessful() || Objects.isNull(storeResponse.getBody())) {
            return ResponseEntity.badRequest().body("查询门店信息失败");
        }


        //到hd验证库存 商品
//        String sku = baseOrderService.checkHdSku(store.getStoreName(), store.getStoreCode(), orderParam.getOrderProducts());
//        if (!"SUCCESS".equals(sku)) {
//            return ResponseEntity.badRequest().body(sku);
//        }
        //TODO  查询商品信息
        List<String> shelfIdList = orderParam.getOrderProducts().parallelStream()
                .map(OrderProductParam::getShelfId).map(String::valueOf).collect(Collectors.toList());


        Store store = storeResponse.getBody();
        OrderStore orderStore = new OrderStore();
        BeanUtils.of(orderStore).populate(store);

        //写库
        OrderDetailResult result = orderService.createOrder(orderParam,new ArrayList<>(),orderStore);
        return ResponseEntity.ok(result);
    }

    @ApiOperation("根据订单id查询订单详情")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "orderCode", dataType = "String", required = true, value = "订单code"),
            @ApiImplicitParam(paramType = "query", name = "needProductList", dataType = "boolean", required = true, value = "是否需要加载商品信息"),
            @ApiImplicitParam(paramType = "query", name = "needOrderFlowList", dataType = "boolean", required = true, value = "是否需要加载订单操作流水信息")
    })
    /**
     * @param orderCode
     * @param needProductList 是否需要加载商品信息
     * @param needOrderFlowList 是否需要加载订单操作流水信息
     */
    @GetMapping("/{orderCode}")
    public ResponseEntity<?> orderDetail(@PathVariable("orderCode") String orderCode, @RequestParam("needProductList") boolean needProductList,
                                         @RequestParam("needOrderFlowList") boolean needOrderFlowList) {
        BaseOrderInfo order = orderService.findByCode(orderCode, needProductList, needOrderFlowList);
        if (Objects.isNull(order)) {
            return ResponseEntity.badRequest().body(Tips.of(-1, "获取订单失败"));
        }
        return ResponseEntity.ok(order);
    }

    @ApiOperation("取消订单")
    @ApiImplicitParam(paramType = "path", name = "orderCode", value = "订单Code", required = true, dataType = "String")
    @PutMapping("/{orderCode}/cancel")
    public ResponseEntity cancelOrder(@PathVariable("orderCode") String orderCode) {
        BaseOrderInfo baseOrderInfo=new BaseOrderInfo();
        baseOrderInfo.setCode(orderCode);
        baseOrderInfo.setStatus(OrderStatus.FAILURE);
        int result =orderService.updateOrderStatusByCode(baseOrderInfo);
        if(result>0){
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().body("更新订单状态为失效失败");
    }

    @ApiOperation("订单退货(包括部分和全部)")
    @ApiImplicitParam(paramType = "path", name = "orderCode", value = "订单orderCode", required = true, dataType = "String")
    @PutMapping("/{orderCode}/refund")
    public ResponseEntity refundOrder(@PathVariable("orderCode") String orderCode,@RequestBody ReturnOrderParam returnOrderParam){
        BaseOrderInfo searchBaseOrderInfo = orderService.findByCode(orderCode);
        if(Objects.isNull(searchBaseOrderInfo)){
            return ResponseEntity.badRequest().body("未找到订单");
        }
        if(Objects.equals(searchBaseOrderInfo.getAllowRefund(),AllowRefund.NO)){
            return ResponseEntity.badRequest().body("订单未非允许退货订单");
        }
        //只允许待发货 已发货 退货中的订单退货
        if(!Objects.equals(searchBaseOrderInfo.getStatus(),OrderStatus.WAIT_SEND_OUT)&&
                !Objects.equals(searchBaseOrderInfo.getStatus(),OrderStatus.SEND_OUT)&&
                !Objects.equals(searchBaseOrderInfo.getStatus(),OrderStatus.RETURNING)){
            return ResponseEntity.badRequest().body("只允许待发货/已发货/退货中的订单退货，当前订单状态为:"+searchBaseOrderInfo.getStatus().getDescription());
        }
        switch (searchBaseOrderInfo.getStatus()){
            case WAIT_SEND_OUT:
                orderService.refundOrderByCode(orderCode,returnOrderParam);
                //TODO 调用海鼎取消订单
                return ResponseEntity.ok("取消待收货订单成功");
            case SEND_OUT:
                //设置为退货中
                orderService.refundOrderApplyByCode(orderCode,returnOrderParam);
                //TODO 发起海鼎退货申请
                return ResponseEntity.ok("发起海鼎退货申请成功");
            case RETURNING:
                //海鼎门店确认收到退货后修改订单为退货完成
                orderService.refundOrderByCode(orderCode,returnOrderParam);
                return ResponseEntity.ok("已收货退货成功");
        }
        return ResponseEntity.badRequest().body("退货未知状态错误");
    }

    @PutMapping("/{orderCode}/dispatching")
    @ApiOperation("修改订单为配送中")
    public ResponseEntity dispatching(@PathVariable("orderCode") String orderCode){
        BaseOrderInfo baseOrderInfo=new BaseOrderInfo();
        baseOrderInfo.setCode(orderCode);
        baseOrderInfo.setStatus(OrderStatus.DISPATCHING);
        int result =orderService.updateOrderStatusByCode(baseOrderInfo);
        if(result>0){
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().body("更新订单状态为配送中失败");
    }

    @ApiOperation("修改订单为已收货")
    @PutMapping(value = "/{orderCode}/received")
    public ResponseEntity received(@PathVariable("orderCode") String orderCode){
        BaseOrderInfo baseOrderInfo=new BaseOrderInfo();
        baseOrderInfo.setCode(orderCode);
        baseOrderInfo.setStatus(OrderStatus.RECEIVED);
        int result =orderService.updateOrderStatusByCode(baseOrderInfo);
        if(result>0){
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().body("更新订单状态为已收货失败");
    }
}
