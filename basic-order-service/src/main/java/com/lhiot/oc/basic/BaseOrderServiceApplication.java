package com.lhiot.oc.basic;

import com.leon.microx.util.SnowflakeId;
import lombok.Data;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

/**
 * @Author zhangfeng created in 2018/9/19 15:12
 **/
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@Data
@ConfigurationProperties(prefix = "oc-modules.basic-order-service.snowflake-id")
public class BaseOrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BaseOrderServiceApplication.class, args);
    }

    private  long workerId;
    private long dataCenterId;

    @Bean
    public SnowflakeId snowflakeId(){
        return new SnowflakeId(workerId,dataCenterId);
    }
}
