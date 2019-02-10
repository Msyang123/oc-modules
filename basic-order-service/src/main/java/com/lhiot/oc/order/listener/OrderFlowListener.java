package com.lhiot.oc.order.listener;

import com.lhiot.oc.order.event.OrderFlowEvent;
import com.lhiot.oc.order.service.OrderFlowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * zhangfeng created in 2018/9/25 12:13
 **/
@Slf4j
@Component
public class OrderFlowListener {
    private OrderFlowService orderFlowService;

    public OrderFlowListener(OrderFlowService orderFlowService) {
        this.orderFlowService = orderFlowService;
    }

    @Async
    @EventListener
    public void onApplicationEvent(OrderFlowEvent orderFlowEvent) {
        orderFlowService.create(orderFlowEvent.getPreStatus(), orderFlowEvent.getOrderId(), orderFlowEvent.getStatus());
    }
}
