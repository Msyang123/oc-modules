package com.lhiot.oc.basic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 配送中心 - 基础服务
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class BasicDeliveryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BasicDeliveryServiceApplication.class, args);
    }
}
