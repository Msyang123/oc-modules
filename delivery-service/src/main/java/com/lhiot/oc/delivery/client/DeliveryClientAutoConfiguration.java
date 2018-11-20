package com.lhiot.oc.delivery.client;

import com.leon.microx.util.Jackson;
import com.lhiot.oc.delivery.client.dada.DadaClient;
import com.lhiot.oc.delivery.client.fengniao.FengNiaoClient;
import com.lhiot.oc.delivery.client.meituan.MeiTuanClient;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Leon (234239150@qq.com) created in 20:19 18.11.13
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(DeliveryClientProperties.class)
public class DeliveryClientAutoConfiguration {

    @Bean
    public DadaAdapter dadaAdapter(DeliveryClientProperties properties) {
        log.info("\t===>>\t[DaDa Delivery Client] initialized");
        DadaClient dadaClient = new DadaClient(properties.getDada(), Jackson::json);
        log.info("\t===>>\t[DaDa Delivery Adapter]  initializing......");
        return new DadaAdapter(dadaClient);
    }

    @Bean
    public FengNiaoAdapter fengNiaoAdapter(DeliveryClientProperties properties, ObjectProvider<RedissonClient> provider) {
        log.info("\t===>>\t[FengNiao Delivery Client] initialized");
        FengNiaoClient fengNiaoClient = new FengNiaoClient(properties.getFengNiao(), provider.getIfAvailable());
        log.info("\t===>>\t[FengNiao Delivery Adapter]  initializing......");
        return new FengNiaoAdapter(fengNiaoClient);
    }

    @Bean
    public MeiTuanAdapter meiTuanAdapter(DeliveryClientProperties properties) {
        log.info("\t===>>\t[MeiTuan Delivery Client] initialized");
        MeiTuanClient meiTuanClient = new MeiTuanClient(properties.getMeiTuan());
        log.info("\t===>>\t[MeiTuan Delivery Adapter]  initializing......");
        return new MeiTuanAdapter(meiTuanClient);
    }
}
