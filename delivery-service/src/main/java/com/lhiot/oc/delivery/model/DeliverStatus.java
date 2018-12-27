package com.lhiot.oc.delivery.model;

import lombok.Getter;

public enum DeliverStatus {

    UNRECEIVE("未接单"),

    WAIT_GET("待取货"),

    DELIVERING("配送中"),

    DONE("配送完成"),

    FAILURE("配送失败");

    @Getter
    private String description;

    DeliverStatus(String description) {
        this.description = description;
    }
}