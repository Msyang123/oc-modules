package com.lhiot.oc.basic.domain.enums;


import lombok.Getter;

public enum DeliverNeedConver {
    YES("需要转成高德系标准 百度坐标系需要"),
    NO("需要转成高德系标准 腾讯坐标系不需要");
    @Getter
    private String  decription;
    DeliverNeedConver(String decription) {
        this.decription = decription;
    }
}
