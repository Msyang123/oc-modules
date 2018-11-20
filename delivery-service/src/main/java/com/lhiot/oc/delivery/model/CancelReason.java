package com.lhiot.oc.delivery.model;

import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * @author Leon (234239150@qq.com) created in 9:08 18.11.11
 */
@Data
@ApiModel
public class CancelReason {
    private Long id;
    private String reason;
}
