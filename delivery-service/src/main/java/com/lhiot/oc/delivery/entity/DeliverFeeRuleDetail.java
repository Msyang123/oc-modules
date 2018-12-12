package com.lhiot.oc.delivery.entity;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author zhangfeng create in 11:50 2018/12/11
 */
@Data
public class DeliverFeeRuleDetail {
    private Long id;

    private Long deliveryFeeRuleId;

    private Integer minDistance;

    private Integer maxDistance;

    private BigDecimal firstWeight;

    private Integer firstFee;

    private BigDecimal additionalWeight;

    private Integer additionalFee;
}
