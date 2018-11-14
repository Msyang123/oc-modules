package com.lhiot.oc.delivery.client.meituan.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 取消订单参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CancelOrderRequest extends AbstractRequest {
    /**
     * 配送活动标识
     */
    private long deliveryId;

    /**
     * 美团配送内部订单id，最长不超过32个字符
     */
    private String mtPeisongId;

    /**
     * 取消原因类别，默认为接入方原因
     */
    private CancelOrderReasonId cancelOrderReasonId;

    /**
     * 详细取消原因，最长不超过256个字符
     */
    private String cancelReason;

    @Override
    public String toString() {
        return "CancelOrderRequest [deliveryId=" + deliveryId + ", mtPeisongId="
                + mtPeisongId + ", cancelOrderReasonId=" + cancelOrderReasonId
                + ", cancelReason=" + cancelReason + "]";
    }
}
