package com.lhiot.oc.basic.domain.enums;

import lombok.Getter;

/**
 * 门店类型枚举
 */
public enum StoreTypeEnum {
    ORDINARY_STORE("普通门店"),
    FLAGSHIP_STORE ("旗舰店");


    @Getter
    private String  decription;

    StoreTypeEnum(String decription) {
        this.decription = decription;
    }
}
