package com.lhiot.oc.basic.domain.enums;

import lombok.Getter;

/**
 * 海鼎状态
 */
public enum  HdStatus {
    SEND_OUT("已发送"),
    SEND_FAILURE("发送失败"),
    NOT_SEND("未发送");

    @Getter
    private String description;

    HdStatus(String description) {
        this.description = description;
    }
}
