package com.lhiot.oc.basic.domain.enums;

/**
 * 订单类型
 */
public enum  OrderType{
    SHOPPING("导购订单"),
    SELF_BUYING("自购订单"),
    TEAM_BUYING("团购订单"),
    PICKING("提货订单"),
    TO_STORE("存入水果仓库");

    private String description;

    OrderType(String description) {
        this.description = description;
    }
}
