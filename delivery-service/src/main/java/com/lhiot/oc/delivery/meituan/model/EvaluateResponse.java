package com.lhiot.oc.delivery.meituan.model;


import lombok.Data;

/**
 * 评价骑手响应类
 */
@Data
public class EvaluateResponse extends AbstractResponse {

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
