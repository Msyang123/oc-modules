package com.lhiot.oc.payment.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotBlank;

@Data
@ToString
@ApiModel(description = "微信支付签名参数")
@EqualsAndHashCode(callSuper = true)
public class WxPayModel extends PayModel {

    @NotBlank(message = "支付项目不能为空")
    @ApiModelProperty(value = "支付商户配置名", dataType = "String", required = true)
    private String configName;

    @NotBlank(message = "客户端IP不能为空")
    @ApiModelProperty(value = "订单生成的机器IP，APP和网页支付传用户浏览器端IP，Native支付传调用微信支付API的机器IP", dataType = "String", required = true)
    private String clientIp;

    @NotBlank(message = "支付回调地址不能为空")
    @ApiModelProperty(value = "支付回调地址", dataType = "String", required = true)
    private String backUrl;

    @ApiModelProperty(value = "订单Code（充值时可选）", dataType = "String")
    private String orderCode;

    @ApiModelProperty(value = "微信openid（微信JSAPI支付时必传）", dataType = "String")
    private String openid;
}
