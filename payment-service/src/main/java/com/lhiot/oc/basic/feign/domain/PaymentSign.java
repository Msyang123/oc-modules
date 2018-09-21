package com.lhiot.oc.delivery.feign.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
* Description:支付签名信息实体类
* @author yijun
* @date 2018/09/20
*/
@Data
@ToString(callSuper = true)
@ApiModel
@NoArgsConstructor
public class PaymentSign{

    /**
    *
    */
    @JsonProperty("id")
    @ApiModelProperty(value = "", dataType = "Long")
    private Long id;

    /**
    *支付商户名称简称
    */
    @JsonProperty("paymentName")
    @ApiModelProperty(value = "支付商户名称简称", dataType = "String")
    private String paymentName;

    /**
    *微信支付APPID/支付宝APPID
    */
    @JsonProperty("partnerId")
    @ApiModelProperty(value = "微信支付APPID/支付宝APPID", dataType = "String")
    private String partnerId;

    /**
    *微信支付秘钥/支付宝公钥
    */
    @JsonProperty("partnerKey")
    @ApiModelProperty(value = "微信支付秘钥/支付宝公钥", dataType = "String")
    private String partnerKey;

    /**
     *支付宝私钥
     */
    @JsonProperty("privateKey")
    @ApiModelProperty(value = "支付宝私钥", dataType = "String")
    private String privateKey;


    /**
    *微信退款签名包
    */
    @JsonProperty("pkcs12Url")
    @ApiModelProperty(value = "微信退款签名包", dataType = "String")
    private String pkcs12Url;

    /**
    *支付宝商户帐号
    */
    @JsonProperty("aliSellerId")
    @ApiModelProperty(value = "支付宝商户帐号", dataType = "String")
    private String aliSellerId;

    /**
    *支付平台
    */
    @JsonProperty("payPlatformType")
    @ApiModelProperty(value = "支付平台", dataType = "String")
    private String payPlatformType;

    /**
    *备注
    */
    @JsonProperty("remark")
    @ApiModelProperty(value = "备注", dataType = "String")
    private String remark;

}
