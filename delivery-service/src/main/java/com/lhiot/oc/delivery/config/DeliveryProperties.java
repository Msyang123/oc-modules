package com.lhiot.oc.delivery.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Leon (234239150@qq.com) created in 9:52 18.9.18
 */
@Data
@ConfigurationProperties(prefix = DeliveryProperties.PREFIX)
public class DeliveryProperties {
    static final String PREFIX = "lhiot.delivery-service";

    private FengNiaoProperties fengNiao;

    private DadaProperties dada;

    private MeiTuanProperties meiTuan;
}

