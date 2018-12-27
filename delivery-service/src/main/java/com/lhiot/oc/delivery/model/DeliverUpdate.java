package com.lhiot.oc.delivery.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author Leon (234239150@qq.com) created in 11:38 18.11.12
 */
@Data
@ApiModel
public class DeliverUpdate {
    @ApiModelProperty(notes = "修改后配送状态",dataType = "DeliverStatus",required = true)
    private DeliverStatus deliverStatus;
    @ApiModelProperty(notes = "配送员",dataType = "String")
    private String carrierDriverName;
    @ApiModelProperty(notes = "配送员联系方式",dataType = "String")
    private String carrierDriverPhone;
    @ApiModelProperty(notes = "配送失败原因",dataType = "String")
    private String failureCause;
}
