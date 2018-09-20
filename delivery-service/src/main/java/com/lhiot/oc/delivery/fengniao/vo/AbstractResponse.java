package com.lhiot.oc.delivery.fengniao.vo;

/**
 * 抽象响应父类
 */
public abstract class AbstractResponse {
    protected String code;
    protected String msg;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
