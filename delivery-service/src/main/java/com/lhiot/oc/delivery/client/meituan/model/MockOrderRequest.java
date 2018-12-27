package com.lhiot.oc.delivery.client.meituan.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模拟订单参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MockOrderRequest extends AbstractRequest {

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
        return "MockOrderRequest [" +
                "deliveryId=" + deliveryId +
                ", mtPeisongId=" + mtPeisongId + "]";
    }
}
