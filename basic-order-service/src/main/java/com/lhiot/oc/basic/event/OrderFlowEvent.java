package com.lhiot.oc.basic.event;

import com.lhiot.oc.basic.model.type.OrderStatus;
import lombok.Data;
import org.springframework.context.ApplicationEvent;

import java.io.Serializable;

/**
 * @Author zhangfeng created in 2018/9/25 12:08
 **/
@Data
public class OrderFlowEvent implements Serializable{
    private OrderStatus preStatus;
    private OrderStatus status;
    private Long orderId;

    public OrderFlowEvent(OrderStatus preStatus,OrderStatus status,Long orderId) {
        this.preStatus = preStatus;
        this.status = status;
        this.orderId = orderId;
    }

//    public OrderStatus getPreStatus() {
//        return preStatus;
//    }
//
//    public OrderStatus getStatus() {
//        return status;
//    }
//
//    public Long getOrderId() {
//        return orderId;
//    }
}
