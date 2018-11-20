package com.lhiot.oc.order.model.type;

/**
 * @author zhangfeng create in 8:38 2018/11/16
 */
public enum  OrderType {

    NORMAL("普通订单"),
    CUSTOM("定制订单"),
    TEAM_BUY("团购订单");

    private String description;

    OrderType(String description) {
        this.description = description;
    }
}
