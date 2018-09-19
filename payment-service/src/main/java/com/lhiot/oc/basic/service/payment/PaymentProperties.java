package com.lhiot.oc.basic.service.payment;

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
     * 发送验证码的第三方推送服务地址
     */
    //private InetRemoteUrl sendSms;

    /**
     * 验证发送验证码服务地址
     */
    //private InetRemoteUrl validateSms;
    /**
     * http连接超时（毫秒数）
     */
    private Integer httpConnectionTimeoutExpress = -1;
    /**
     * 临时订单时效毫秒（订单从待支付到支付完成的有效毫秒数，过期则修改订单状态为失效）
     */
    private long temporaryOrderExpirationMs;

    private WeChatOauth weChatOauth;

    @Data
    public static  class WeChatOauth {
        /**
         * 支付超时（分钟。最短失效时间间隔必须大于5分钟）
         */
        private Integer timeoutExpress = 6;
        /**
         * APPID
         */
        private String appId;
        /**
         * APP密钥
         */
        //private String appSecret;

        /**
         * 授权后跳转到的地址
         */
        //private String appRedirectUri;

        /**
         * 调整到前端页面的地址
         */
        //private String appFrontUri;
    }

    private WeChatPayConfig weChatPay;

    @Data
    public static  class WeChatPayConfig {
        /**
         * 支付超时（分钟。最短失效时间间隔必须大于5分钟）
         */
        private Integer timeoutExpress = 6;

        private Lhiot lhiot;

        private Qiguoguo qiguoguo;
        @Data
        public static  class Lhiot{
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
        }

        @Data
        public static  class Qiguoguo{
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
        }

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

        /**
         * 区分支付来源为正式还是测试
         */
        private String env;
    }

    /**
     * 支付宝配置
     * 绿航
     */

    private AliPayConfig aliPay;
    @Data
    public static final class AliPayConfig {
        /**
         * api接口网关地址
         */
        private String apiUrl;
        /**
         * 超时时间（分钟）
         */
        private Integer timeoutExpress = 6;

        /**
         * 签名类型（RSA2）
         */
        private String signType = AlipayConstants.SIGN_TYPE_RSA2;

        private Lhiot lhiot;

        private Qiguoguo qiguoguo;

        @Data
        public static  class Lhiot{
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
             * 应用私钥
             */
            private String appPrivateKey;
        }

        @Data
        public static  class Qiguoguo{
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
             * 应用私钥
             */
            private String appPrivateKey;
        }

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

/*    @Data
    public static class InetRemoteUrl {
        private String url;
        private String version;

        public <T> HttpEntity<T> createRequest(T parameters) {
            return this.createRequest(parameters, null);
        }

        public <T> HttpEntity<T> createRequest(T parameters, Map<String, String> extHeaders) {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
            headers.setAccept(Arrays.asList(MediaType.ALL, MediaType.APPLICATION_JSON_UTF8));
            headers.set("version", version);
            if (!CollectionUtils.isEmpty(extHeaders)) {
                extHeaders.forEach(headers::set);
            }
            return new HttpEntity<>(parameters, headers);
        }
    }*/
}
