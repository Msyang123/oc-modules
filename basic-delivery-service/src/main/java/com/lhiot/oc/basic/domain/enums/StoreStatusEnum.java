package com.lhiot.oc.basic.domain.enums;

import lombok.Getter;

/**
 * 门店状态枚举
 */
public enum StoreStatusEnum {
    ENABLED("营业"),
    DISABLED("未营业");


    @Getter
    private String  decription;

    StoreStatusEnum(String decription) {
        this.decription = decription;
    }
}
