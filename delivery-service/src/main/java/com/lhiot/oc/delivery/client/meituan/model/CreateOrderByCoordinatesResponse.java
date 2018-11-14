package com.lhiot.oc.delivery.client.meituan.model;


import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 订单创建(送货分拣方式)响应类
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CreateOrderByCoordinatesResponse extends AbstractResponse {

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
