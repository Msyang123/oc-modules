package com.lhiot.oc.basic.domain;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.lhiot.oc.basic.domain.enums.ApplicationTypeEnum;
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

    @JsonProperty("orderId")
    private Long orderId;

    @JsonProperty("userId")
    private Long userId;

    //应用类型 小程序 app等
    @JsonProperty("appType")
    private ApplicationTypeEnum applicationType;

    //来源类型 充值 订单
    @JsonProperty("sourceType")
    private String sourceType;

    //全额支付还是混合支付
    @JsonProperty("payType")
    private String payType;

    //支付步骤枚举字符串
    @JsonProperty("payStep")
    private String payStep;

    @JsonProperty("payFee")
    private Integer payFee;

    @JsonProperty("tradeId")
    private String tradeId;

    @JsonProperty("signAt")
    private Timestamp signAt;

    @JsonProperty("payAt")
    private Timestamp payAt;

    @JsonProperty("bankType")
    private String bankType;

    @JsonProperty("deleted")
    private String deleted;

    @JsonProperty("orderCode")
    private String orderCode;


}
