package com.lhiot.oc.delivery.meituan.model;


import lombok.Data;

/**
 * 查询订单状态响应类
 */
@Data
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
