package com.lhiot.oc.payment.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotBlank;

@Data
@ToString(callSuper = true)
@ApiModel(description = "余额支付参数")
@EqualsAndHashCode(callSuper = true)
public class BalancePayModel extends PayModel {

    @NotBlank(message = "余额支付订单code不能为空")
    @ApiModelProperty(value = "订单Code", notes = "充值是调用第三方支付，此处与充值无关，只作余额支付订单使用，所以为必填", dataType = "String", required = true)
    private String orderCode;

    @ApiModelProperty(notes = "支付密码",dataType = "String")
    private String password;
}
