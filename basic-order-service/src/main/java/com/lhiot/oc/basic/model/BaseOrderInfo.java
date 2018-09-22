package com.lhiot.oc.basic.model;


import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.Date;
import java.util.List;

@Data
@ApiModel
@EqualsAndHashCode(callSuper = false)
public class BaseOrderInfo{

    @ApiModelProperty(notes = "id", dataType = "int64")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String code;
    private Long userId;
    private ApplicationType applicationType;
    private ReceivingWay receivingWay;
    private Integer totalAmount;
    private Integer amountPayable;
    private Integer deliveryAmount;
    private Integer couponAmount;
    private HdStatus hdStatus = HdStatus.NOT_SEND;
    private OrderStatus status = OrderStatus.WAIT_PAYMENT;
    private Date createAt = Date.from(Instant.now());
    @ApiModelProperty(notes = "收货人", dataType = "String")
    private String receiveUser;
    @ApiModelProperty(notes = "收货人联系方式", dataType = "String")
    private String contactPhone;
    @ApiModelProperty(notes = "收货地址：门店自提订单填写门店地址", dataType = "String")
    private String address;
    private String remark;
    @ApiModelProperty(notes = "提货截止时间", dataType = "String")
    private Date deliveryEndTime;
    private Date hdStockAt;
    private String hdOrderCode;
    @ApiModelProperty(notes = "用户昵称", dataType = "String")
    private String nickname;

    @ApiModelProperty(notes = "配送时间段", dataType = "String")
    private String deliverTime;
    @ApiModelProperty(notes = "是否允许退款YES是NO否", dataType = "AllowRefund")
    private AllowRefund allowRefund = AllowRefund.YES;

    @ApiModelProperty(notes = "订单商品列表", dataType = "List")
    private List<OrderProduct> orderProductList;

    @ApiModelProperty(notes = "订单状态流水列表", dataType = "List")
    private List<OrderFlow> orderFlowList;
}
