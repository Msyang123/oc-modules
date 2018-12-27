package com.lhiot.oc.delivery.client.meituan.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询订单参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QueryOrderRequest extends AbstractRequest {

    /**
     * 配送活动标识
     */
    private Long deliveryId;

    /**
     * 美团配送内部订单id，最长不超过32个字符
     */
    private String mtPeisongId;

    @Override
    public String toString() {
        return "QueryOrderRequest [" +
                "deliveryId=" + deliveryId +
                ", mtPeisongId=" + mtPeisongId + "]";
    }
}
