package com.lhiot.oc.delivery.api.imitaor;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

/**
 * @author zhangfeng create in 12:27 2018/11/26
 */
@Service
public class ImitatorDeliveryService implements ApplicationContextAware {

    private ApplicationContext context;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
        this.context = applicationContext;
    }

    public ImitatorAdaptableClient adapt(ImitatorDeliverType deliverType) {
        return (ImitatorAdaptableClient) context.getBean(deliverType.getClientClass());
    }
}
