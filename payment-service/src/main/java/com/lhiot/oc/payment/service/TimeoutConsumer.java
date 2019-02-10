package com.lhiot.oc.payment.service;

import com.leon.microx.exception.InternalException;
import com.leon.microx.pay.Payments;
import com.lhiot.oc.payment.entity.Record;
import com.lhiot.oc.payment.feign.PaymentConfig;
import com.lhiot.oc.payment.type.PayStep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Slf4j
@Component
@Transactional
public class TimeoutConsumer {

    public static final String DEFAULT_PAY_TIMEOUT_EXCHANGE_NAME = "oc-payment-timeout-exchange";

    public static final String DEFAULT_PAY_TIMEOUT_DLX_QUEUE_NAME = "oc-payment-timeout-dlx-queue";

    public static final String DEFAULT_PAY_TIMEOUT_DLX_RECEIVE_NAME = "oc-payment-timeout-receive-queue";

    private final PaymentService service;

    @Autowired
    public TimeoutConsumer(PaymentService service) {
        this.service = service;
    }

    @RabbitHandler
    @Transactional
    @RabbitListener(queues = DEFAULT_PAY_TIMEOUT_DLX_RECEIVE_NAME)
    public void timeout(String outTradeId) {
        Record record = service.record(Long.valueOf(outTradeId));
        if (Objects.isNull(record)) {
            log.warn("未找到支付单 " + outTradeId);
            return;
        }
        if (!Objects.equals(record.getPayStep(), PayStep.SIGN)) {
            log.error("支付单 " + outTradeId + " 不处于签名状态，无法超时取消！");
            return;
        }
        PaymentConfig config = service.findPaymentConfig(record.getConfigName());
        if (Objects.isNull(config)) {
            log.error("无法载入商户配置 " + record.getConfigName() + "，无法超时取消！");
            return;
        }

        boolean updated = service.timeout(Long.valueOf(outTradeId));
        if (!updated) {
            log.error("修改支付单 " + outTradeId + " 超时失败，不调用第三方关单接口");
            return;
        }
        boolean operated;
        switch (record.getTradeType()) {
            case ALI_APP:
                operated = Payments.ali(config.getAppId(), config.getAppSecretKey(), config.getThirdPartyKey())
                        .close(outTradeId, null);
                break;
            case WX_JS_API:
                operated = Payments.wx(config.getAppId(), config.getAppSecretKey(), config.getMerchantId(), config.getMerchantSecretKey())
                        .close(outTradeId);
                break;
            case WX_APP:
                operated = Payments.wx(config.getAppId(), config.getAppSecretKey(), config.getMerchantId(), config.getMerchantSecretKey())
                        .close(outTradeId);
                break;
            default:
                log.error("不支持的TradeType：" + record.getTradeType());
                return;
        }
        if (!operated) {
            throw new InternalException("数据库支付单已改为超时，但未调用第三方关单接口！！！");
        }
    }
}
