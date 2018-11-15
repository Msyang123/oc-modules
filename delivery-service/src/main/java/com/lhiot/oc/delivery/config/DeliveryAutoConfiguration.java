package com.lhiot.oc.delivery.config;

import com.leon.microx.util.Jackson;
import com.lhiot.oc.delivery.dada.DadaDeliverHelper;
import com.lhiot.oc.delivery.dada.DadaDeliveryClient;
import com.lhiot.oc.delivery.fengniao.FengNiaoDeliveryClient;
import com.lhiot.oc.delivery.meituan.MeiTuanDeliveryClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Leon (234239150@qq.com) created in 9:50 18.9.18vo
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(DeliveryProperties.class)
public class DeliveryAutoConfiguration {

    @Bean
    public DadaDeliverHelper dadaDeliverHelper(DeliveryProperties properties) {
        return new DadaDeliverHelper(properties.getDada());
    }

    @Bean
    public DadaDeliveryClient dadaDeliveryClient(DadaDeliverHelper helper) {
        log.info("\t===>>\t[DaDa Delivery Service] initializing......");
        return new DadaDeliveryClient(helper, Jackson::json);
    }

    @Bean
    public FengNiaoDeliveryClient fengNiaoDeliveryClient(DeliveryProperties properties){
        log.info("\t===>>\t[FengNiao Delivery Service] initializing......");
        return new FengNiaoDeliveryClient(properties.getFengNiao());
    }

    @Bean
    public MeiTuanDeliveryClient meiTuanDeliveryClient(DeliveryProperties properties){
        log.info("\t===>>\t[FengNiao Delivery Service] initializing......");
        return new MeiTuanDeliveryClient(properties.getMeiTuan());
    }
}
