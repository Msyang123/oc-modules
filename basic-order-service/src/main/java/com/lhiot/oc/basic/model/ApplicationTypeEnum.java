package com.lhiot.oc.basic.model;

import lombok.Getter;

/**
 * 应用类型
 */
public enum ApplicationTypeEnum {
    APP("视食"),
    WECHAT_MALL("微商城"),
    S_MALL("小程序"),
    FRUIT_DOCTOR("鲜果师商城"),
    WXSMALL_SHOP("微商城小程序");
    @Getter
    private String description;

    ApplicationTypeEnum(String description) {
        this.description = description;
    }
}
