package com.lhiot.oc.basic.model;

import com.lhiot.oc.basic.model.type.OrderRefundStatus;
import com.lhiot.oc.basic.model.type.RefundType;
import lombok.Data;

import java.time.Instant;
import java.util.Date;

/**
 * @Author zhangfeng created in 2018/9/25 17:16
 **/
@Data
public class OrderRefund {
    private Long id;
    private Long orderId;
    private String hdOrderCode;
    private Long userId;
    private String orderProductIds;
    private String reason;
    private OrderRefundStatus orderRefundStatus;
    private RefundType refundType;
    private Date applyAt = Date.from(Instant.now());
    private Date disposeAt;
}
