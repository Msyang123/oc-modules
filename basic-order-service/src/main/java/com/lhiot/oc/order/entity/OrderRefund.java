package com.lhiot.oc.order.entity;

import com.lhiot.oc.order.entity.type.OrderRefundStatus;
import com.lhiot.oc.order.entity.type.RefundType;
import lombok.Data;

import java.util.Date;

/**
 * @author zhangfeng created in 2018/9/25 17:16
 **/
@Data
public class OrderRefund {
    private Long id;
    private Long orderId;
    private String hdOrderCode;
    private Long userId;
    private String orderProductIds;
    private String reason;
    private OrderRefundStatus refundStatus;
    private RefundType refundType;
    private Date applyAt;
    private Date disposeAt;
    private Integer fee;
}
