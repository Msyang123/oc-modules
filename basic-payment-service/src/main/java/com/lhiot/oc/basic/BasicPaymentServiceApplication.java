package com.lhiot.oc.basic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 支付中心 - 基础服务
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class BasicPaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BasicPaymentServiceApplication.class, args);
    }
}