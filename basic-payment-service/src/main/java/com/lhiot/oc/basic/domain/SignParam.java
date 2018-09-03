package com.lhiot.oc.basic.domain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@ApiModel
@NoArgsConstructor
/**
 * 支付签名参数
 */
public class SignParam {

    @ApiModelProperty(value = "微信openid 支付宝支付不需要", dataType = "String")
    private String openid;

    @ApiModelProperty(value = "支付金额(充值时需要传入)", dataType = "int")
    private int fee;

    @ApiModelProperty(value = "订单编码", dataType = "String")
    private String orderCode;

    @ApiModelProperty(value = "支付项目", dataType = "String")
    private String memo;

    @ApiModelProperty(value = "支付平台", dataType = "PayPlatformType")
    private PayPlatformType payPlatformType;
    public enum PayPlatformType{
        WEIXIN("微信支付"),
        ALIPAY("支付宝支付"),
        BALANCE("余额支付");

        @Getter
        private String decription;

        PayPlatformType(String decription) {
            this.decription = decription;
        }
    }
    @ApiModelProperty(value = "自定义参数", dataType = "Attach")
    private Attach attach;
}
