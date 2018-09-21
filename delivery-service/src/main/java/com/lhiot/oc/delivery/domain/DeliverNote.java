package com.lhiot.oc.delivery.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lhiot.oc.delivery.domain.common.PagerRequestObject;
import com.lhiot.oc.delivery.domain.enums.DeliverType;
import com.lhiot.oc.delivery.domain.enums.DeliveryStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;

/**
* Description:配送单信息实体类
* @author zhangshu
* @date 2018/08/06
*/
@EqualsAndHashCode(callSuper = false)
@Data
@ToString
@ApiModel
@NoArgsConstructor
public class DeliverNote extends PagerRequestObject {

    /**
    *id
    */
    @JsonProperty("id")
    @ApiModelProperty(value = "id", dataType = "Long")
    private Long id;

    /**
    *配送单编码
    */
    @JsonProperty("deliverCode")
    @ApiModelProperty(value = "配送单编码", dataType = "String")
    private String deliverCode;

    /**
    *订单id
    */
    @JsonProperty("orderId")
    @ApiModelProperty(value = "订单id", dataType = "Long")
    private Long orderId;

    /**
    *创建时间
    */
    @JsonProperty("createTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间", dataType = "Date")
    private Date createTime;
    

    /**
    *配送员姓名
    */
    @JsonProperty("deliverName")
    @ApiModelProperty(value = "配送员姓名", dataType = "String")
    private String deliverName;

    /**
    *配送员手机号
    */
    @JsonProperty("deliverPhone")
    @ApiModelProperty(value = "配送员手机号", dataType = "String")
    private String deliverPhone;

    /**
    *配送距离
    */
    @JsonProperty("distance")
    @ApiModelProperty(value = "配送距离", dataType = "Double")
    private Double distance;

    /**
    *配送费
    */
    @JsonProperty("fee")
    @ApiModelProperty(value = "配送费", dataType = "Integer")
    private Integer fee;

    /**
    *订单编码
    */
    @JsonProperty("orderCode")
    @ApiModelProperty(value = "订单编码", dataType = "String")
    private String orderCode;

    /**
    *配送方式 FENGNIAO-蜂鸟配送DADA-达达配送 OWN-自己配送
    */
    @JsonProperty("deliverType")
    @ApiModelProperty(value = "配送方式 FENGNIAO-蜂鸟配送DADA-达达配送 OWN-自己配送", dataType = "DeliverType")
    private DeliverType deliverType;

    /**
    *配送失败原因
    */
    @JsonProperty("failureCause")
    @ApiModelProperty(value = "配送失败原因", dataType = "String")
    private String failureCause;

    /**
    *配送取消时间
    */
    @JsonProperty("cancelTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value = "配送取消时间", dataType = "Date")
    private Date cancelTime;
    

    /**
    *接单时间
    */
    @JsonProperty("receiveTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value = "接单时间", dataType = "Date")
    private Date receiveTime;
    

    /**
    *配送状态 UNRECEIVE-未接单 WAIT_GET-待取货 TRANSFERING-配送中 DONE-配送完成 FAILURE-配送失败
    */
    @JsonProperty("deliverStatus")
    @ApiModelProperty(value = "配送状态 UNRECEIVE-未接单 WAIT_GET-待取货 TRANSFERING-配送中 DONE-配送完成 FAILURE-配送失败", dataType = "DeliveryStatus")
    private DeliveryStatus deliverStatus;

    /**
     *门店编码
     */
    @JsonProperty("storeCode")
    @ApiModelProperty(value = "门店编码", dataType = "String")
    private String storeCode;

    /**
     *备注
     */
    @JsonProperty("remark")
    @ApiModelProperty(value = "备注", dataType = "String")
    private String remark;

}
