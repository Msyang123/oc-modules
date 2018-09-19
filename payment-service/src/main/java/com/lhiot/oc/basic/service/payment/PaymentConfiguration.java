package com.lhiot.oc.basic.service.payment;

import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConstants;
import com.alipay.api.DefaultAlipayClient;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@RefreshScope
@Configuration
@EnableConfigurationProperties(PaymentProperties.class)
public class PaymentConfiguration {
    /**
     * 支付宝支付
     *
     * @param properties 配置类
     * @return AliPayClient bean
     */
    @Bean
    @ConditionalOnMissingBean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public AlipayClient alipayClient(PaymentProperties properties) {
        PaymentProperties.AliPayConfig config = properties.getAliPay();
        return new DefaultAlipayClient(
                config.getApiUrl(),
                config.getLhiot().getAppId(),
                config.getLhiot().getAppPrivateKey(),
                AlipayConstants.FORMAT_JSON,
                properties.getCharset(),
                config.getLhiot().getAliPayPublicKey(),
                config.getSignType()
        );
    }
}
