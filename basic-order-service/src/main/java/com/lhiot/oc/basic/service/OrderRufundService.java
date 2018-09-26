package com.lhiot.oc.basic.service;

import com.lhiot.oc.basic.mapper.OrderRefundMapper;
import com.lhiot.oc.basic.model.OrderRefund;

/**
 * @Author zhangfeng created in 2018/9/25 18:34
 **/
public class OrderRufundService {
    private OrderRefundMapper orderRefundMapper;

    public OrderRufundService(OrderRefundMapper orderRefundMapper) {
        this.orderRefundMapper = orderRefundMapper;
    }

    public int create(OrderRefund orderRefund){
       return orderRefundMapper.insert(orderRefund);
    }
}
