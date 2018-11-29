package com.lhiot.oc.order.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.lhiot.oc.order.entity.type.AllowRefund;
import com.lhiot.oc.order.entity.type.HdStatus;
import com.lhiot.oc.order.entity.type.OrderStatus;
import com.lhiot.oc.order.entity.type.ReceivingWay;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.Instant;
import java.util.Date;

/**
 * @author zhangfeng created in 2018/9/22 17:00
 **/
@Data
@ApiModel
public class BaseOrder {
    @ApiModelProperty(notes = "id", dataType = "int64")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String code;
    private Long userId;
    private String orderType;
    private String applicationType;
    private ReceivingWay receivingWay;
    private Integer totalAmount;
    private Integer amountPayable;
    private Integer deliveryAmount;
    private Integer couponAmount;
    private HdStatus hdStatus;
    private OrderStatus status;
    private Date createAt = Date.from(Instant.now());
    @ApiModelProperty(notes = "收货人", dataType = "String")
    private String receiveUser;
    @ApiModelProperty(notes = "收货人联系方式", dataType = "String")
    private String contactPhone;
    @ApiModelProperty(notes = "收货地址：门店自提订单填写门店地址", dataType = "String")
    private String address;
    private String remark;
    @ApiModelProperty(notes = "提货截止时间", dataType = "String")
    private Date deliveryEndAt;
    private Date hdStockAt;
    private String hdOrderCode;
    @ApiModelProperty(notes = "用户昵称", dataType = "String")
    private String nickname;

    @ApiModelProperty(notes = "配送时间段", dataType = "String")
    private String deliverAt;
    @ApiModelProperty(notes = "是否允许退款YES是NO否", dataType = "AllowRefund")
    private AllowRefund allowRefund;
}
