package com.lhiot.oc.basic.domain;

import com.lhiot.oc.basic.domain.enums.ApplicationType;
import com.lhiot.oc.basic.domain.enums.PayPlatformType;
import com.lhiot.oc.basic.domain.enums.SourceType;
import com.lhiot.oc.basic.feign.domain.OperationStatus;
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
 * 支付签名参数(微信 支付宝 鲜果币统一)
 */
public class SignParam {

    @ApiModelProperty(value = "微信openid 支付宝支付不需要", dataType = "String")
    private String openid;

    @ApiModelProperty(value = "自定义支付编码", dataType = "String")
    private String payCode;

    @ApiModelProperty(value = "支付金额(分)", dataType = "Long")
    private Long fee;

    @ApiModelProperty(value = "支付项目", dataType = "String")
    private String memo;

    @ApiModelProperty(value = "支付平台", dataType = "PayPlatformType")
    private PayPlatformType payPlatformType;

    @ApiModelProperty(notes = "加减操作标识(用于鲜果币操作)：SUBTRACT - 减，ADD-加", dataType = "OperationStatus")
    private OperationStatus operation;

    @ApiModelProperty(value = "baseuserId 用于充值及鲜果币支付", dataType = "Long")
    private Long baseuserId;
    //业务用户id
    @ApiModelProperty(value = "userId", dataType = "Long")
    private Long userId;
    @ApiModelProperty(value = "应用类型", dataType = "ApplicationType")
    private ApplicationType applicationType;

    @ApiModelProperty(value = "支付类型", dataType = "SourceType")
    private SourceType sourceType;

    @ApiModelProperty(value = "支付回调地址（鲜果币支付不需要传递）", dataType = "String")
    private String backUrl;



}
