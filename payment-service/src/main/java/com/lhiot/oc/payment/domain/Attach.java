package com.lhiot.oc.payment.domain;

import com.lhiot.oc.payment.domain.enums.ApplicationType;
import com.lhiot.oc.payment.domain.enums.SourceType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@ApiModel
@NoArgsConstructor
/**
 * 第三方签名自定义参数
 */
public class Attach {

    @ApiModelProperty(value = "baseuserId 用于充值", dataType = "Long")
    private Long baseuserId;
    //微信用户业务用户id
    @ApiModelProperty(value = "userId", dataType = "Long")
    private Long userId;
    @ApiModelProperty(value = "应用类型", dataType = "ApplicationType")
    private ApplicationType applicationType;
    @ApiModelProperty(value = "支付类型", dataType = "SourceType")
    private SourceType sourceType;

    @ApiModelProperty(value = "支付商户名称简称", dataType = "String")
    private String paymentName;

    @ApiModelProperty(value = "鲜果币支付密码", dataType = "String")
    private String paymentPassword;

}
