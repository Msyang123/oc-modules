package com.lhiot.oc.basic.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.lhiot.oc.basic.domain.common.PagerRequestObject;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.sql.Timestamp;

/**
* 描述：
* @author yijun
* @date 2018-07-21
*/
@Data
@ToString
@ApiModel
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class OrderRefund extends PagerRequestObject implements Serializable {

    /**
    *订单商品id
    */
    @JsonProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(notes = "订单商品id", dataType = "Long")
    private Long id;

    /**
    *订单id
    */
    @JsonProperty("orderId")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(notes = "订单id", dataType = "Long")
    private Long orderId;

    /**
    *
    */
    @JsonProperty("orderCode")
    @ApiModelProperty(notes = "", dataType = "String")
    private String orderCode;

    /**
    *退货原因
    */
    @JsonProperty("reason")
    @ApiModelProperty(notes = "退货原因", dataType = "String")
    private String reason;

    /**
    *退货时间
    */
    @JsonProperty("refundAt")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(notes = "退货时间", dataType = "Timestamp")
    private Timestamp refundAt;

    /**
    *用户id
    */
    @JsonProperty("userId")
    @ApiModelProperty(notes = "用户id", dataType = "Long")
    private Long userId;

    /**
    *
    */
    @JsonProperty("refundFee")
    @ApiModelProperty(notes = "", dataType = "String")
    private String refundFee;

    /**
    *
    */
    @JsonProperty("transactionNo")
    @ApiModelProperty(notes = "", dataType = "String")
    private String transactionNo;

}
