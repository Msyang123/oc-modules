package com.lhiot.oc.delivery.service.payment;

import com.alipay.api.AlipayConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = PaymentProperties.PROPERTIES_PREFIX)
public class PaymentProperties {

    public static final String PROPERTIES_PREFIX = "payment";
    /**
     * 编码
     */
    private String charset = "UTF-8";
    /**
     * http连接超时（毫秒数）
     */
    private Integer httpConnectionTimeoutExpress = -1;
    /**
     * 临时订单时效毫秒（订单从待支付到支付完成的有效毫秒数，过期则修改订单状态为失效）
     */
    private long temporaryOrderExpirationMs;

    /**
     *
     */
    private WeChatPayConfig weChatPayConfig;
    /**
     * 支付宝配置
     */
    private AliPayConfig aliPayConfig;

    @Data
    public static  class WeChatPayConfig {
        /**
         * 支付超时（分钟。最短失效时间间隔必须大于5分钟）
         */
        private Integer timeoutExpress = 6;

        /**
         * 商户号
         */
        private String partnerId;
        /**
         * 商户密钥
         */
        private String partnerKey;
        /**
         * pkcs12证书
         */
        private Resource pkcs12;


        /**
         * 微信支付回调统一地址
         */
        private String notifyUrl;

        /**
         * 支付换算
         * # 1：1倍，单位还是分； 100：100倍，单位就变成元了
         */
        private Integer payunit;

        /**
         * 前端静态页使用代理
         */
        private String proxy;
    }


    @Data
    public static final class AliPayConfig{
        /**
         * api接口网关地址
         */
        private String apiUrl;
        /**
         * 超时时间（分钟）
         */
        private String timeoutExpress;

        /**
         * 签名类型（RSA2）
         */
        private String signType = AlipayConstants.SIGN_TYPE_RSA2;


        /**
         * 商户帐号
         */
        private String sellerId;
        /**
         * APPID
         */
        private String appId;
        /**
         * 支付宝公钥
         */
        private String aliPayPublicKey;

        /**
         * 支付宝私钥
         */
        private String aliPayPrivateKey;

        /**
         * 异步回调地址
         */
        private String notifyUrl;

        /**
         * 调用支付宝取消支付异步回调地址
         *
         */
        private String cancelNotifyUrl;
    }
}
