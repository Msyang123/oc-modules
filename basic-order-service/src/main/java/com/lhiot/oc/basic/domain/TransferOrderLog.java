package com.lhiot.oc.basic.domain;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.sql.Timestamp;

/**
 * 调货
 */
@Data
@ApiModel
public class TransferOrderLog {
    private Long id;
    private Long orderId;
    private Long storeId;
    private String orderCode;
    private Long operationUser;
    private Timestamp createAt;
}
