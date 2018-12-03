package com.lhiot.oc.payment.alipay;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

/**
 * @author Leon (234239150@qq.com) created in 11:49 18.11.30
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
    private String appPrivateKey;

    /**
     * 商户ID
     */
    private String merchantId;

    /**
     * 支付宝公钥
     */
    private String aliPublicKey;


    /**
     * 支付完成 - 回调地址
     */
    private String payedNotifyUrl;

    /**
     * 支付撤销 - 回调地址
     */
    private String cancelNotifyUrl;

    /**
     * 退款 - 回调地址
     */
    private String refundNotifyUrl;
}
