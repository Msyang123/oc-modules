package com.lhiot.oc.delivery.client;

import com.lhiot.oc.delivery.client.dada.DadaClient;
import com.lhiot.oc.delivery.client.fengniao.FengNiaoClient;
import com.lhiot.oc.delivery.client.meituan.MeiTuanClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Leon (234239150@qq.com) created in 20:19 18.11.13
 */
@Data
@ConfigurationProperties(prefix = DeliveryClientProperties.PREFIX)
public class DeliveryClientProperties {
    static final String PREFIX = "lhiot.delivery-service";

    private FengNiaoClient.Config fengNiao;

    private DadaClient.Config dada;

    private MeiTuanClient.Config meiTuan;
}
