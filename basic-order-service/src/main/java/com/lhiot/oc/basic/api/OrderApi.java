package com.lhiot.oc.basic.api;

import com.leon.microx.support.result.Tips;
import com.leon.microx.util.BeanUtils;
import com.leon.microx.util.StringUtils;
import com.lhiot.oc.basic.feign.BaseServiceFeign;
import com.lhiot.oc.basic.model.StoreInfo;
import com.lhiot.oc.basic.model.*;
import com.lhiot.oc.basic.service.OrderService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @Author zhangfeng created in 2018/9/19 9:42
 **/
@RestController
@Slf4j
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
        ResponseEntity<StoreInfo> storeResponse = baseServiceFeign.storeById(orderParam.getStoreId(),orderParam.getApplicationType());
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

        baseServiceFeign.findProductByProductIdList(StringUtils.arrayToDelimitedString(shelfIdList.toArray(),","));
        StoreInfo store = storeResponse.getBody();
        OrderStore orderStore = new OrderStore();
        BeanUtils.of(orderStore).populate(store);

        //写库
        OrderDetailResult result = orderService.createOrder(orderParam,new ArrayList<>(),orderStore);
        return ResponseEntity.ok(result);
    }
}
