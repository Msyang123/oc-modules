package com.lhiot.oc.basic.model;

import com.lhiot.oc.basic.model.common.PagerRequestObject;
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
    private OrderStatus status;
    private OrderStatus preStatus;
    private Timestamp createAt;
}
