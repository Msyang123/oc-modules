package com.lhiot.oc.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 订单中心 - 基础服务
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class BasicOrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BasicOrderServiceApplication.class, args);
    }
}
