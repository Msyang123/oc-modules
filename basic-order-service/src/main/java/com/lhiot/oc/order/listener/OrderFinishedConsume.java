package com.lhiot.oc.order.listener;

import com.leon.microx.exception.ServiceException;
import com.leon.microx.util.Maps;
import com.lhiot.oc.order.entity.type.OrderStatus;
import com.lhiot.oc.order.mapper.BaseOrderMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author zhangfeng create in 11:26 2018/12/25
 */
@Slf4j
@Component
public class OrderFinishedConsume {

    @Autowired
    private BaseOrderMapper orderMapper;

    public OrderFinishedConsume() {
        log.info("=================================");
    }

    @RabbitHandler
    @RabbitListener(queues = "basic-order-OrderFinishedConsume-receive")
    public void handle(String orderCode) {
        log.info("延时队列生效！！！！");
        int count = orderMapper.updateStatusByCode(Maps.of("nowStatus", OrderStatus.RECEIVED, "modifyStatus", OrderStatus.FINISHED
                , "orderCode", orderCode));
        if (count != 1) {
            throw new ServiceException("订单:" + orderCode + "修改为已完成失败");
        }
    }
}
