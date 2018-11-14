package com.lhiot.oc.delivery.model;


import com.lhiot.oc.delivery.client.DadaAdapter;
import com.lhiot.oc.delivery.client.FengNiaoAdapter;
import com.lhiot.oc.delivery.client.MeiTuanAdapter;
import lombok.Getter;

import java.util.stream.Stream;

public enum DeliverType {

    FENGNIAO(FengNiaoAdapter.class, "蜂鸟配送"),

    DADA(DadaAdapter.class, "达达配送"),

    MEITUAN(MeiTuanAdapter.class, "美团配送"),

    OWN(Void.class, "自己配送");

    @Getter
    private Class<?> clientClass;

    @Getter
    private String description;

    DeliverType(Class<?> clientClass, String description) {
        this.clientClass = clientClass;
        this.description = description;
    }

    public static DeliverType matches(Class<?> clientClass) {
        return Stream.of(values()).filter(type -> clientClass.equals(type.clientClass)).findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}
