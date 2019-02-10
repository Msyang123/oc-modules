package com.lhiot.oc.delivery.client.meituan.model;


import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 取消订单响应类
 */
@Data
@EqualsAndHashCode(callSuper = true)
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
