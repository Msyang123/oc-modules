package com.lhiot.oc.delivery.client.dada.model;

import lombok.Data;
/**
 * 达达配送添加订单正确返回结果对象
 */
@Data
public class OrderAdded {
    private String status;

    private int code;

    private String msg;

    private Result result;

    @Data
    private class Result {
        private double distance;
        private double fee;
        private double deliverFee;
    }
}