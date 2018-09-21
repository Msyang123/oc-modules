package com.lhiot.oc.payment.feign.domain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Author zhangfeng created in 2018/9/20 16:29
 **/
@Data
@ApiModel
public class PaymentPasswordParam {
    @ApiModelProperty(notes = "用户Id",dataType = "Long")
    private Long userId;
    @ApiModelProperty(notes = "支付密码",dataType = "String")
    private String paymentPassword;
}
