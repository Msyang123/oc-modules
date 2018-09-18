package com.lhiot.oc.basic.domain.enums;

import lombok.Getter;

public enum DeliveryStatus {
    UNRECEIVE("未接单"),
    WAIT_GET("待取货"),
    TRANSFERING("配送中"),
    DONE("配送完成"),
    FAILURE("配送失败");

    @Getter
    private String  decription;
    DeliveryStatus(String decription) {
        this.decription = decription;
    }
}