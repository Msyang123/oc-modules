package com.lhiot.oc.order.model.type;

import lombok.Getter;

/**
 * @author zhangfeng create in 10:33 2018/11/30
 */
public enum ApplicationType {
    APP("APP", "视食APP"),
    WECHAT_MALL("WM", "微商城"),
    WECHAT_SMALL_SHOP("WSS", "小程序"),
    HEALTH_GOOD("HG", "鲜果师"),
    NEW_RETAIL("NR", "新零售"),

    ;

    @Getter
    private String ref;
    private String description;

    ApplicationType(String ref, String description) {
        this.ref = ref;
        this.description = description;
    }

    public static String ref(String name) {
        try {
            return valueOf(name).ref;
        } catch (IllegalArgumentException e) {
            return "";
        }
    }
}
