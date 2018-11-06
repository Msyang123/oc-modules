package com.lhiot.oc.delivery.domain.enums;


import lombok.Getter;

//配送状态 UNRECEIVE-未接单 WAIT_GET-待取货 DELIVERING-配送中 DONE-配送完成 FAILURE-配送失败
//配送方式 FENGNIAO-蜂鸟配送DADA-达达配送 OWN-自己配送
public enum DeliverType {
    FENGNIAO("蜂鸟配送DADA"),
    DADA("达达配送"),
    MEITUAN("美团配送"),
    OWN("自己配送");
    @Getter
    private String  decription;
    DeliverType(String decription) {
        this.decription = decription;
    }
}
