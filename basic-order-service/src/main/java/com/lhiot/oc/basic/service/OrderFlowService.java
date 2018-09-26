package com.lhiot.oc.basic.service;

import com.lhiot.oc.basic.mapper.OrderFlowMapper;
import com.lhiot.oc.basic.model.OrderFlow;
import com.lhiot.oc.basic.model.type.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
@Transactional
public class OrderFlowService {
    private OrderFlowMapper orderFlowMapper;

    public OrderFlowService(OrderFlowMapper orderFlowMapper) {
        this.orderFlowMapper = orderFlowMapper;
    }

    /**
     * 记录订单状态流水
     *
     * @param preStatus 订单当前状态
     * @param orderId   订单Id
     * @param status    修改后的状态
     */
    public void create(OrderStatus preStatus, Long orderId, OrderStatus status) {

        OrderFlow orderFlow = new OrderFlow();
        orderFlow.setOrderId(orderId);
        orderFlow.setStatus(status);
        orderFlow.setPreStatus(preStatus);
        orderFlow.setCreateAt(Date.from(Instant.now()));
        orderFlowMapper.create(orderFlow);
    }

    //根据订单id查询
    public List<OrderFlow> flowByOrderId(Long orderId) {
        return orderFlowMapper.flowByOrderId(orderId);
    }
}
