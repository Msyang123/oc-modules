package com.lhiot.oc.delivery.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lhiot.oc.delivery.domain.common.PagerRequestObject;
import com.lhiot.oc.delivery.domain.enums.DeliveryStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
* Description:配送状态流转记录实体类
* @author yijun
* @date 2018/09/16
*/
@Data
@ToString(callSuper = true)
@ApiModel
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DeliverFlow extends PagerRequestObject {

    /**
    *id
    */
    @JsonProperty("id")
    @ApiModelProperty(value = "id", dataType = "Long")
    private Long id;

    /**
    *配送单id
    */
    @JsonProperty("deliverNoteId")
    @ApiModelProperty(value = "配送单id", dataType = "Long")
    private Long deliverNoteId;

    /**
    *当前状态
    */
    @JsonProperty("status")
    @ApiModelProperty(value = "当前状态", dataType = "DeliveryStatus")
    private DeliveryStatus status;

    /**
    *上一步状态
    */
    @JsonProperty("preStatus")
    @ApiModelProperty(value = "上一步状态", dataType = "DeliveryStatus")
    private DeliveryStatus preStatus;

    /**
    *创建时间
    */
    @JsonProperty("createAt")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间", dataType = "Date")
    private java.util.Date createAt;
    

}
