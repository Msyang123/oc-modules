package com.lhiot.oc.delivery;

import com.leon.microx.util.SnowflakeId;
import lombok.Data;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.util.Assert;

/**
 * 订单中心 - 配送服务
 */
@Data
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@ConfigurationProperties("lhiot.delivery-service.snowflake")
public class DeliveryServiceApplication {

    private long workerId;
    private long dataCenterId;

    @Bean
    public SnowflakeId snowflakeId() {
        Assert.isTrue(this.workerId > 0 && this.dataCenterId > 0, "SnowflakeId 初始化失败！");
        return new SnowflakeId(workerId, dataCenterId);
    }

    public static void main(String[] args) {
        SpringApplication.run(DeliveryServiceApplication.class, args);
    }
}
