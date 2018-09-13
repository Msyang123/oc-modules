package com.lhiot.oc.basic.domain.enums;

public enum RefundStatus {
    REFUND("已退货"),
    NOT_REFUND("未退货");

    private String description;

    RefundStatus(String description) {
        this.description = description;
    }
}
