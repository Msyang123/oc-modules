package com.lhiot.oc.basic.domain.enums;

import lombok.Getter;

public enum SourceType{
    RECHARGE("充值"),
    ORDER("订单"),
    ACTIVITY("活动");
    @Getter
    private String decription;

    SourceType(String decription) {
        this.decription = decription;
    }
}