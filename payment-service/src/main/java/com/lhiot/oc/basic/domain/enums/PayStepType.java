package com.lhiot.oc.basic.domain.enums;

import lombok.Getter;

public enum PayStepType {
    SIGN("签名"),
    NOTIFY("响应"),
    REFUND("退款");
    @Getter
    private String decription;

    PayStepType(String decription) {
        this.decription = decription;
    }
}