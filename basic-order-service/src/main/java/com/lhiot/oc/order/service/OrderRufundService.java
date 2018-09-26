package com.lhiot.oc.order.service;

import com.lhiot.oc.order.mapper.OrderRefundMapper;
import com.lhiot.oc.order.model.OrderRefund;

/**
 * @Author zhangfeng created in 2018/9/25 18:34
 **/
public class OrderRufundService {
    private OrderRefundMapper orderRefundMapper;

    public OrderRufundService(OrderRefundMapper orderRefundMapper) {
        this.orderRefundMapper = orderRefundMapper;
    }

    public int create(OrderRefund orderRefund) {
        return orderRefundMapper.insert(orderRefund);
    }
}
