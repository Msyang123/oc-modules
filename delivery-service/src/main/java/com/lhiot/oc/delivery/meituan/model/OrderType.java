package com.lhiot.oc.delivery.meituan.model;

/**
 * 订单类型
 */
public enum OrderType {
    NORMAL(0), // 及时单
    PREBOOK(1); // 预约单

    private final int code;

    private OrderType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

}
