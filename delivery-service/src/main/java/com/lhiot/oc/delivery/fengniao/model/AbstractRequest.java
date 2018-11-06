package com.lhiot.oc.delivery.fengniao.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.leon.microx.util.Jackson;

import java.io.IOException;
import java.net.URLEncoder;

/**
 * 抽象request类
 */

public abstract class AbstractRequest {
    //商户App Id
    @JsonProperty("app_id")
    private String appId;
    protected int salt;
    protected String signature;
    private AbstractRequestData data;

    public void setData(AbstractRequestData data) {
        this.data = data;
    }
    public String getData() throws IOException {
        return URLEncoder.encode(Jackson.json(this.data),"UTF-8");
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public int getSalt() {
        return salt;
    }

    public void setSalt(int salt) {
        this.salt = salt;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    @Override
    public String toString() {
        return "AbstractRequest{" +
                "appId='" + appId + '\'' +
                ", salt=" + salt +
                ", signature='" + signature + '\'' +
                ", data=" + data +
                '}';
    }
}
