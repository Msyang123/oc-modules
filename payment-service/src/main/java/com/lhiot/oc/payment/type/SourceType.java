package com.lhiot.oc.payment.type;

import lombok.Getter;

public enum SourceType {

    RECHARGE("充值"),

    ORDER("订单"),

    ACTIVITY("活动");

    @Getter
    private String description;

    SourceType(String description) {
        this.description = description;
    }
}