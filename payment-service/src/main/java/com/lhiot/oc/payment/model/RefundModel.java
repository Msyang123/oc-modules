package com.lhiot.oc.payment.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.DecimalMin;

/**
 * @author Leon (234239150@qq.com) created in 11:53 18.12.5
 */
@Data
@ToString
@ApiModel(description = "退款参数")
public class RefundModel {

    @DecimalMin(value = "1", message = "支付金额必须大于0")
    @ApiModelProperty(value = "支付金额(分)", dataType = "Long", required = true)
    private Long fee;

    @ApiModelProperty(value = "退款原因", dataType = "String")
    private String reason;

    @ApiModelProperty(value = "退款回调地址", dataType = "String")
    private String notifyUrl;
}
