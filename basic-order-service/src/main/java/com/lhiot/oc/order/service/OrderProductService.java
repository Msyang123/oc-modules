package com.lhiot.oc.order.service;

import com.leon.microx.util.Maps;
import com.lhiot.oc.order.mapper.OrderProductMapper;
import com.lhiot.oc.order.entity.OrderProduct;
import com.lhiot.oc.order.entity.type.RefundStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@Transactional
public class OrderProductService {

    private OrderProductMapper orderProductMapper;

    public OrderProductService(OrderProductMapper orderProductMapper) {
        this.orderProductMapper = orderProductMapper;
    }

    public int batchInsert(List<OrderProduct> orderProducts) {
        return orderProductMapper.batchInsert(orderProducts);
    }


    //修改订单商品状态
    public boolean updateOrderProductByIds(Long orderId, RefundStatus refundStatus, List<String> orderProductIds) {
        Map<String, Object> param = Maps.of("orderId", orderId,
                "refundStatus", refundStatus,
                "orderProductIds", orderProductIds);
        return orderProductMapper.updateOrderProductByIds(param) > 0;
    }

    public List<OrderProduct> findOrderProductListByIds(List<String> idList){
       return orderProductMapper.selectOrderProductsByIds(idList);
    }
}
