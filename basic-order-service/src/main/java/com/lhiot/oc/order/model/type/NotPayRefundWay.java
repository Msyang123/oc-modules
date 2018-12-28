package com.lhiot.oc.order.model.type;

public enum NotPayRefundWay {
    NOT_SEND_HD("未发送海鼎"),
    NOT_STOCKING("发送海鼎未备货"),
    STOCKING("海鼎已备货"),
    ;

    private String description;

    NotPayRefundWay(String description) {
        this.description = description;
    }
}
