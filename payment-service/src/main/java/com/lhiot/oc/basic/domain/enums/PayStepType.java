package com.lhiot.oc.basic.domain.enums;

import lombok.Getter;

public enum PayStepType {
    SIGN("签名"),
    notify("响应");
    @Getter
    private String decription;

    PayStepType(String decription) {
        this.decription = decription;
    }
}