package com.lhiot.oc.payment.feign;

import io.swagger.annotations.ApiModelProperty;
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
     * 配置名称
     */
    private String configName;

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
}
