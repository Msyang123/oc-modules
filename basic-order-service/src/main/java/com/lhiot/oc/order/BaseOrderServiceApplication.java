package com.lhiot.oc.order;

import com.leon.microx.amqp.ConsumeExceptionResolver;
import com.leon.microx.amqp.RabbitInitializer;
import com.leon.microx.probe.collector.ProbeEventPublisher;
import com.leon.microx.util.Maps;
import lombok.Data;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.Arrays;

/**
 * @author zhangfeng created in 2018/9/19 15:12
 **/
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableAsync
@Data
public class BaseOrderServiceApplication {

    @Bean
    public ConsumeExceptionResolver exceptionResolver(ProbeEventPublisher publisher) {
        return (joinPoint, rabbitListener, exception) -> publisher.mqConsumerException(exception, Maps.of(
                "queue", Arrays.asList(rabbitListener.queues()).toString(),
                "target", joinPoint.getTarget().toString()
        ));
    }

    @Bean
    public Object init(RabbitInitializer initializer) {
        initializer.delay("basic-order.finished-delay", "basic-order-OrderFinishedConsume-dlx", "basic-order-OrderFinishedConsume-receive");
        return null;
    }

    public static void main(String[] args) {
        SpringApplication.run(BaseOrderServiceApplication.class, args);
    }
}
