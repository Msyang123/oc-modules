package com.lhiot.oc.delivery.meituan.model;

/**
 * 取消原因id枚举
 */
public enum CancelOrderReasonId {
    PARTNER_REASON(199, "其他接入方原因"), // 接入方其他原因
    DELIVER_REASON(299, "其他美团配送原因"), // 配送方其他原因
    OTHER(399, "其他原因"); // 其他原因

    private int code;
    private String msg;

    private CancelOrderReasonId(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public static CancelOrderReasonId findByCode(int code) {
        for (CancelOrderReasonId type : CancelOrderReasonId.values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        return null;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
