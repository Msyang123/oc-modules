package com.lhiot.oc.payment.domain.enums;

import lombok.Getter;

public enum PayPlatformType{
    WE_CHAT("微信支付"),
    ALIPAY("支付宝支付"),
    BALANCE("余额支付");

    @Getter
    private String decription;

    PayPlatformType(String decription) {
        this.decription = decription;
    }
}