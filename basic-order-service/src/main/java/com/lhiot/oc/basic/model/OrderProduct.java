package com.lhiot.oc.basic.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @Author zhangfeng created in 2018/9/19 10:51
 **/
@Data
public class OrderProduct {
    private Long id;
    private Long orderId;
    private Long specificationId;
    private String barcode;
    private Long totalPrice;
    private Long productQty;
    private BigDecimal specificationQty;
    private RefundStatus refundStatus;
    private Long discountPrice;
    private String productName;
    private String image;
    private BigDecimal totalWeight;
    private Long sourceId;
    private String sourceType;
}
