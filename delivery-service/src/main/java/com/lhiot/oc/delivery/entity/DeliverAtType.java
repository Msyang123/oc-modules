package com.lhiot.oc.delivery.entity;

import lombok.Getter;

public enum DeliverAtType {
    DAYTIME("8:00","18:00"),
    EVENING("18:00","22:00"),
    ALL_DAY("8:00","22:00"),
    ;


    @Getter
    private String startAt;
    @Getter
    private String endAt;

    DeliverAtType(String startAt, String endAt) {
        this.startAt = startAt;
        this.endAt = endAt;
    }
}
