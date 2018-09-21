package com.lhiot.oc.payment.domain;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.lhiot.oc.payment.domain.enums.ApplicationType;
import com.lhiot.oc.payment.domain.enums.PayPlatformType;
import com.lhiot.oc.payment.domain.enums.PayStepType;
import com.lhiot.oc.payment.domain.enums.SourceType;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.sql.Timestamp;

@Data
@ToString
@ApiModel
@NoArgsConstructor
public class PaymentLog {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("baseUserId")
    private Long baseUserId;

    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("payCode")
    private String payCode;

    //应用类型 小程序 app等
    @JsonProperty("applicationType")
    private ApplicationType applicationType;

    //来源类型 充值 订单 活动
    @JsonProperty("sourceType")
    private SourceType sourceType;

    //支付平台
    @JsonProperty("payPlatformType")
    private PayPlatformType payPlatformType;

    //支付步骤枚举
    @JsonProperty("payStep")
    private PayStepType payStep;

    //支付费用（分）
    @JsonProperty("fee")
    private Long fee;

    //第三方单号
    @JsonProperty("tradeId")
    private String tradeId;

    @JsonProperty("signAt")
    private Timestamp signAt;

    @JsonProperty("payAt")
    private Timestamp payAt;

    @JsonProperty("bankType")
    private String bankType;

    //支付商户名称简称
/*    @JsonProperty("paymentName")
    private String paymentName;*/
}
