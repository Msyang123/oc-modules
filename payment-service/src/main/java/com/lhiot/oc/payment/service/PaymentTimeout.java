package com.lhiot.oc.payment.service;

import com.leon.microx.amqp.RabbitInitializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentTimeout {

    private static final String DEFAULT_PAY_TIMEOUT_EXCHANGE_NAME = "oc-payment-timeout-exchange";

    private static final String DEFAULT_PAY_TIMEOUT_DLX_QUEUE_NAME = "oc-payment-timeout-dlx-queue";

    static final String DEFAULT_PAY_TIMEOUT_DLX_RECEIVE_NAME = "oc-payment-timeout-receive-queue";

    private final RabbitTemplate rabbit;

    @Autowired
    public PaymentTimeout(RabbitInitializer initializer, RabbitTemplate rabbit) {
        initializer.delay(
                PaymentTimeout.DEFAULT_PAY_TIMEOUT_EXCHANGE_NAME,
                PaymentTimeout.DEFAULT_PAY_TIMEOUT_DLX_QUEUE_NAME,
                PaymentTimeout.DEFAULT_PAY_TIMEOUT_DLX_RECEIVE_NAME
        );
        this.rabbit = rabbit;
    }

    public void delay(Long outTradeId, long ttlMs) {
        rabbit.convertAndSend(DEFAULT_PAY_TIMEOUT_EXCHANGE_NAME, DEFAULT_PAY_TIMEOUT_DLX_QUEUE_NAME, String.valueOf(outTradeId), message -> {
            message.getMessageProperties().setExpiration(String.valueOf(ttlMs));
            return message;
        });
    }

}
