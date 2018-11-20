package com.lhiot.oc.delivery.model;

import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * @author Leon (234239150@qq.com) created in 11:38 18.11.12
 */
@Data
@ApiModel
public class DeliverUpdate {
    private String orderId;
    private DeliverStatus deliverStatus;
    private String carrierDriverName;
    private String carrierDriverPhone;
    private String cancelReason;
}
