package com.lhiot.oc.basic.domain;

import com.lhiot.order.domain.common.PagerRequestObject;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.sql.Timestamp;

/**
 * 订单操作流水记录
 */
@EqualsAndHashCode(callSuper = false)
@Data
@ApiModel
public class OrderFlow extends PagerRequestObject {
    private Long id;
    private Long orderId;
    private String status;
    private String preStatus;
    private Timestamp createAt;
}
