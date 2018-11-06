package com.lhiot.oc.delivery.meituan.model;


import lombok.Data;

/**
 * 取消订单响应类
 */
@Data
public class CancelOrderResponse extends AbstractResponse {

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
