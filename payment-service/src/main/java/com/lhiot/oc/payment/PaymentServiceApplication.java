package com.lhiot.oc.payment;


import com.leon.microx.amqp.ConsumeExceptionResolver;
import com.leon.microx.amqp.RabbitInitRunner;
import com.leon.microx.amqp.RabbitInitializer;
import com.leon.microx.probe.collector.ProbeEventPublisher;
import com.leon.microx.util.Maps;
import com.lhiot.oc.payment.service.TimeoutConsumer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

/**
 * 支付中心 - 基础服务
 */
@EnableFeignClients
@SpringBootApplication
@EnableDiscoveryClient
@EnableTransactionManagement
public class PaymentServiceApplication {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public RabbitInitRunner rabbitInitRunner(RabbitInitializer initializer) {
        return args -> initializer.delay(
                TimeoutConsumer.DEFAULT_PAY_TIMEOUT_EXCHANGE_NAME,
                TimeoutConsumer.DEFAULT_PAY_TIMEOUT_DLX_QUEUE_NAME,
                TimeoutConsumer.DEFAULT_PAY_TIMEOUT_DLX_RECEIVE_NAME
        );
    }

    @Bean
    public ConsumeExceptionResolver exceptionResolver(ProbeEventPublisher publisher) {
        return (joinPoint, rabbitListener, exception) -> publisher.mqConsumerException(exception, Maps.of(
                "queue", Arrays.asList(rabbitListener.queues()).toString(),
                "target", joinPoint.getTarget().toString()
        ));
    }

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
