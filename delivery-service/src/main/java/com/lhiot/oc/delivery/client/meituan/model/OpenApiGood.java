package com.lhiot.oc.delivery.client.meituan.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单商品信息
 */
@Data
public class OpenApiGood {
    /**
     * 商品数量
     */
    private int goodCount;

    /**
     * 商品名称
     */
    private String goodName;

    /**
     * 商品价格，单位为元
     */
    private BigDecimal goodPrice;

    /**
     * 商品单位，如个
     */
    private String goodUnit;

    @Override
    public String toString() {
        return "OpenApiGood {" +
                "goodCount=" + goodCount +
                ", goodPrice=" + goodPrice +
                ", goodName=" + goodName +
                ", goodUnit=" + goodUnit +
                "}";
    }
}


