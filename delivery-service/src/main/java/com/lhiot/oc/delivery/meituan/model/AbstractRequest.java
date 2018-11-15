package com.lhiot.oc.delivery.meituan.model;

import lombok.Data;

/**
 * 抽象request类
 */
@Data
public abstract class AbstractRequest {

    /**
     * 配送开放平台为每个合作方分配独立的appkey，作为合作方接入认证标识。
     * 每个appkey会绑定一个secret，用于计算签名。请妥善保管secret，避免泄密。如果secret意外泄露，可要求重新生成。
     */
    protected String appkey;

    /**
     * 时间戳，格式为long，时区为GMT+8，即合作方调用接口时距离Epoch（1970年1月1日) 以秒计算的时间（unix-timestamp）。
     * 开放平台允许合作方请求最大时间误差为10分钟（配送开放平台接到请求的时间 - 合作方调用接口的时间 < 10分钟）。
     */
    protected long timestamp;

    /**
     * API协议版本，可选值：1.0。
     */
    protected String version;

    /**
     * API请求参数的签名计算结果。
     */
    protected String sign;
}
