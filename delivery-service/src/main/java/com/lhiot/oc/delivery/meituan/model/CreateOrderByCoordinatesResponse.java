package com.lhiot.oc.delivery.meituan.model;


import lombok.Data;

/**
 * 订单创建(送货分拣方式)响应类
 */
@Data
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
