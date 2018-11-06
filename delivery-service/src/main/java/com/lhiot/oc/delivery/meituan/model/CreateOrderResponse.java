package com.lhiot.oc.delivery.meituan.model;

import lombok.Data;

/**
 * 创建订单响应类
 */
@Data
public class CreateOrderResponse extends AbstractResponse {

    private OrderIdInfo data;

    @Override
    public String toString() {
        return "CreateOrderResponse {" +
                "code=" + code +
                ", message=" + message +
                ", data=" + data +
                '}';
    }
}
