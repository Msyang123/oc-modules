package com.lhiot.oc.delivery.client.meituan.model;

import lombok.Data;

import java.util.List;

/**
 * 订单商品信息列表
 */
@Data
public class OpenApiGoods {
    private List<OpenApiGood> goods;


    @Override
    public String toString() {
        return "OpenApiGoods {" +
                "goods=" + goods +
                "}";
    }
}


