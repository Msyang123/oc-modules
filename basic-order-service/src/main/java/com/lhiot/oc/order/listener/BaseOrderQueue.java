package com.lhiot.oc.order.listener;

import com.leon.microx.amqp.RabbitInitializer;
import lombok.Data;
import lombok.Getter;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.Serializable;

/**
 * @author zhangfeng create in 11:45 2018/12/25
 */
public interface BaseOrderQueue {

    String getExchange();

    String getDlx();

    String getReceive();

    default void init(RabbitInitializer initializer) {
        initializer.delay(this.getExchange(), this.getDlx(), this.getReceive());
    }

    default void send(RabbitTemplate delegate, Serializable data, long delay) {
        delegate.convertAndSend(this.getExchange(), this.getDlx(), data, message -> {
            message.getMessageProperties().setExpiration(String.valueOf(delay));
            return message;
        });
    }

    enum DelayQueue implements BaseOrderQueue {
        AUTO_FINISHED("basic-order-service.finished-delay", "basic-order-service.finished-delay.order-finished-consume.dlx", DelayQueue.AUTO_FINISHED_CONSUME),
        ;

        public static final String AUTO_FINISHED_CONSUME = "basic-order-service.finished-delay.order-finished-consume.auto-finished";
        @Getter
        private final String exchange;//信道
        @Getter
        private final String dlx;//死信队列
        @Getter
        private final String receive;//消费队列

        DelayQueue(String exchange, String dlx, String receive) {
            this.exchange = exchange;
            this.dlx = dlx;
            this.receive = receive;
        }
    }

}
