package com.lhiot.oc.delivery.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lhiot.oc.delivery.domain.common.PagerRequestObject;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
* Description:支付退款记录实体类
* @author yijun
* @date 2018/09/20
*/
@Data
@ToString(callSuper = true)
@ApiModel
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PaymentRefund extends PagerRequestObject {

    /**
    *
    */
    @JsonProperty("id")
    @ApiModelProperty(value = "", dataType = "Long")
    private Long id;

    /**
    *
    */
    @JsonProperty("paymentLogId")
    @ApiModelProperty(value = "", dataType = "Long")
    private Long paymentLogId;

    /**
    *创建时间
    */
    @JsonProperty("createTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间", dataType = "Date")
    private java.util.Date createTime;
    

    /**
    *退款理由
    */
    @JsonProperty("refundMemo")
    @ApiModelProperty(value = "退款理由", dataType = "String")
    private String refundMemo;

}
