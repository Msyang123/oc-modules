package com.lhiot.oc.payment.feign;

import lombok.Data;
import lombok.ToString;

/**
 * @author Leon (234239150@qq.com) created in 16:18 18.11.24
 */
@Data
@ToString
public class PaymentConfig {

    /**
     * 配置ID（客户端使用此ID获取配置信息）
     */
    private String id;

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 应用密钥
     */
    private String appSecretKey;

    /**
     * 商户ID
     */
    private String merchantId;

    /**
     * 商户密钥（微信有，支付宝使用的sdk不需要传）
     */
    private String merchantSecretKey;

    /**
     * 第三方密钥（支付宝私钥 / 微信pkcs12文件URL）
     */
    private String thirdPartyKey;

    /**
     * 支付完成 - 回调地址
     */
    private String payedNotifyUrl;

    /**
     * 支付异常 - 取消本次支付
     */
    private String cancelNotifyUrl;

    /**
     * 退款 - 回调地址
     */
    private String refundNotifyUrl;

//    /**
//     * 备注
//     */
//    private String remark;
//
//    public com.lhiot.oc.payment.alipay.Config toAliPayConfig(){
//        return com.lhiot.oc.payment.alipay.Config.builder()
//                .appId(this.appId)
//                .appPrivateKey(this.appSecretKey)
//                .merchantId(this.merchantId)
//                .aliPublicKey(this.thirdPartyKey)
//                .payedNotifyUrl(this.payedNotifyUrl)
//                .cancelNotifyUrl(this.cancelNotifyUrl)
//                .refundNotifyUrl(this.refundNotifyUrl)
//                .build();
//    }
//
//    public com.lhiot.oc.payment.wxpay.Config toWxPayConfig(){
//        return com.lhiot.oc.payment.wxpay.Config.builder()
//                .appId(this.appId)
//                .appSecret(this.appSecretKey)
//                .partnerId(this.merchantId)
//                .partnerKey(this.merchantSecretKey)
//                .certificate(loadCertificate(this.thirdPartyKey))
//                .payedNotifyUrl(this.payedNotifyUrl)
//                .refundNotifyUrl(this.refundNotifyUrl)
//                .build();
//    }
//
//    private InputStream loadCertificate(String pkcs12){
//
//        return null;
//    }
}
