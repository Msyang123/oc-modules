package com.lhiot.oc.payment.wxpay;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.io.InputStream;

/**
 * @author Leon (234239150@qq.com) created in 9:57 18.12.1
 */
@Data
@Builder
@ToString
public class Config {
    /**
     * 应用ID
     */
    private String appId;
    /**
     * 应用密钥
     */
    private String appSecret;

    /**
     * 商户ID
     */
    private String partnerId;

    /**
     * 商户密钥
     */
    private String partnerKey;

    /**
     * pkcs12证书
     */
    private InputStream certificate;

    /**
     * 支付完成 - 回调地址
     */
    private String payedNotifyUrl;

    /**
     * 退款 - 回调地址
     */
    private String refundNotifyUrl;
}
