package com.lhiot.oc.basic.domain;

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

    @ApiModelProperty(value = "微信应用appid 支付宝支付不需要", dataType = "String")
    private String appid;

    @ApiModelProperty(value = "自定义支付编码", dataType = "String")
    private String payCode;

    @ApiModelProperty(value = "支付金额(分)", dataType = "Long")
    private Long fee;

    @ApiModelProperty(value = "支付项目", dataType = "String")
    private String memo;

    @ApiModelProperty(value = "支付回调地址（鲜果币支付不需要传递）", dataType = "String")
    private String backUrl;

    @ApiModelProperty(value = "附加参数", dataType = "Attach")
    private Attach attach;


}
