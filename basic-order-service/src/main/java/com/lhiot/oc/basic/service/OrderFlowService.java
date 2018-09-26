package com.lhiot.oc.basic.service;

import com.lhiot.oc.basic.mapper.OrderFlowMapper;
import com.lhiot.oc.basic.model.BaseOrderInfo;
import com.lhiot.oc.basic.model.OrderFlow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

@Service
@Slf4j
@Transactional
public class OrderFlowService {
    private OrderFlowMapper orderFlowMapper;

    public OrderFlowService(OrderFlowMapper orderFlowMapper) {
        this.orderFlowMapper = orderFlowMapper;
    }

    public int create(BaseOrderInfo searchBaseOrderInfo, BaseOrderInfo baseOrderInfo) {

        OrderFlow orderFlow = new OrderFlow();
        orderFlow.setOrderId(baseOrderInfo.getId());
        orderFlow.setStatus(baseOrderInfo.getStatus());
        orderFlow.setPreStatus(searchBaseOrderInfo==null?null:searchBaseOrderInfo.getStatus());
        orderFlow.setCreateAt(new Timestamp(System.currentTimeMillis()));
        return orderFlowMapper.create(orderFlow);
    }

    //根据订单id查询
    public List<OrderFlow> flowByOrderId(Long orderId) {
        return orderFlowMapper.flowByOrderId(orderId);
    }
}
