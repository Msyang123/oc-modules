package com.lhiot.oc.basic.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lhiot.oc.basic.domain.common.PagerRequestObject;
import com.lhiot.oc.basic.domain.enums.PayStepType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
* Description:支付状态流转记录实体类
* @author yijun
* @date 2018/09/20
*/
@Data
@ToString(callSuper = true)
@ApiModel
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PaymentFlow extends PagerRequestObject {

    /**
    *id
    */
    @JsonProperty("id")
    @ApiModelProperty(value = "id", dataType = "Long")
    private Long id;

    /**
    *支付记录id
    */
    @JsonProperty("paymentLogId")
    @ApiModelProperty(value = "支付记录id", dataType = "Long")
    private Long paymentLogId;

    /**
    *当前状态
    */
    @JsonProperty("status")
    @ApiModelProperty(value = "当前状态", dataType = "PayStepType")
    private PayStepType status;

    /**
    *上一步状态
    */
    @JsonProperty("preStatus")
    @ApiModelProperty(value = "上一步状态", dataType = "PayStepType")
    private PayStepType preStatus;

    /**
    *创建时间
    */
    @JsonProperty("createAt")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间", dataType = "Date")
    private java.util.Date createAt;
    

}
