package com.lhiot.oc.delivery.api.imitaor;

import com.lhiot.oc.delivery.client.FengNiaoAdapter;
import com.lhiot.oc.delivery.client.MeiTuanAdapter;
import lombok.Getter;

/**
 * @author zhangfeng create in 15:13 2018/11/26
 */
public enum ImitatorDeliverType {
    FENGNIAO(FengNiaoAdapter.class, "蜂鸟配送"),

    DADA(ImitatorDadaAdapter.class, "达达配送"),

    MEITUAN(MeiTuanAdapter.class, "美团配送"),

    @Deprecated
    OWN(Void.class, "自己配送");

    @Getter
    private Class<?> clientClass;

    @Getter
    private String description;

    ImitatorDeliverType(Class<?> clientClass, String description) {
        this.clientClass = clientClass;
        this.description = description;
    }
}
