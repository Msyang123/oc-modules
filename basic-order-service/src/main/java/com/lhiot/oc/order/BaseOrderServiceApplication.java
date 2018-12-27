package com.lhiot.oc.order;

import lombok.Data;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * @author zhangfeng created in 2018/9/19 15:12
 **/
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableAsync
@Data
public class BaseOrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BaseOrderServiceApplication.class, args);
    }
}
