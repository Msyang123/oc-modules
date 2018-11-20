package com.lhiot.oc.delivery.client.meituan.model;


import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询订单状态响应类
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QueryOrderResponse extends AbstractResponse {

    private OrderStatusInfo data;

    @Override
    public String toString() {
        return "CreateOrderResponse {" +
                "code=" + code +
                ", message=" + message +
                ", data=" + data +
                '}';
    }
}
