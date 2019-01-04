package com.lhiot.oc.payment.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotBlank;

@Data
@ToString(callSuper = true)
@ApiModel(description = "蚂蚁金服支付签名参数")
@EqualsAndHashCode(callSuper = true)
public class AliPayModel extends PayModel {

    @NotBlank(message = "支付项目不能为空")
    @ApiModelProperty(value = "支付商户配置名", dataType = "String", required = true)
    private String configName;

    @NotBlank(message = "支付回调地址不能为空")
    @ApiModelProperty(value = "支付回调地址", dataType = "String", required = true)
    private String backUrl;

    @ApiModelProperty(value = "订单Code（充值时可选）", dataType = "String")
    private String orderCode;
}
