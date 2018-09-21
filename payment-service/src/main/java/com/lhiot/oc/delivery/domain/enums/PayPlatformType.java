package com.lhiot.oc.delivery.domain.enums;

import lombok.Getter;

public enum PayPlatformType{
    WEIXIN("微信支付"),
    ALIPAY("支付宝支付"),
    BALANCE("余额支付");

    @Getter
    private String decription;

    PayPlatformType(String decription) {
        this.decription = decription;
    }
}