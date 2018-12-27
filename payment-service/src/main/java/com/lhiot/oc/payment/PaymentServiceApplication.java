package com.lhiot.oc.payment;


import com.leon.microx.amqp.ConsumeExceptionResolver;
import com.leon.microx.probe.collector.ProbeEventPublisher;
import com.leon.microx.util.Maps;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

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
