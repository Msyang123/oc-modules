package com.lhiot.oc.basic.domain;


import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.lhiot.oc.basic.domain.common.PagerRequestObject;
import com.lhiot.oc.basic.domain.enums.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Data
@ApiModel
@EqualsAndHashCode(callSuper = false)
public class BaseOrderInfo extends PagerRequestObject {

    @ApiModelProperty(notes = "id", dataType = "int64")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String code;
    private Long userId;
    private Apply apply;
    private OrderType orderType;
    private Long storeId;
    private String storeCode;
    private String storeName;
    private ReceivingWay receivingWay;
    private Integer totalAmount;
    private Integer amountPayable;
    private Integer deliveryAmount;
    private Integer couponAmount;
    private HdStatus hdStatus;
    private OrderStatus status;
    @ApiModelProperty(notes = "收货地址：门店自提订单填写门店地址", dataType = "String")
    private String address;
    @ApiModelProperty(notes = "退款原因", dataType = "String")
    private String reason;
    private Timestamp createAt;

    @ApiModelProperty(notes = "收货人", dataType = "String")
    private String receiveUser;
    @ApiModelProperty(notes = "收货人联系方式", dataType = "String")
    private String contactPhone;
    private String remark;
    @ApiModelProperty(notes = "提货截止时间", dataType = "String")
    private Timestamp deliveryEndTime;
    private Timestamp hdStockAt;
    private String hdOrderCode;

    @ApiModelProperty(notes = "订单中商品/套餐件数", dataType = "Integer")
    private Integer productCount;
    @ApiModelProperty(notes = "用户昵称", dataType = "String")
    private String nickname;

    @ApiModelProperty(notes = "配送时间段", dataType = "String")
    private String deliverTime;
    @ApiModelProperty(notes = "配送时间", dataType = "Timestamp")
    private Timestamp deliveryTime;
    @ApiModelProperty(notes = "送达时间", dataType = "Timestamp")
    private Timestamp recieveTime;
    @ApiModelProperty(notes = "经度", dataType = "String")
    private String coordy;
    @ApiModelProperty(notes = "纬度", dataType = "String")
    private String coordx;
    @ApiModelProperty(notes = "是否允许退款YES是NO否", dataType = "String")
    private String allowRefund;
    @ApiModelProperty(notes = "优惠券id", dataType = "Long")
    private Long couponId;

    @ApiModelProperty(notes = "坐标位置-经度", dataType = "String")
    private Double storeCoordy;
    @ApiModelProperty(notes = "坐标位置-纬度", dataType = "String")
    private Double storeCoordx;
    //配送时间
    private Timestamp deliverytime;

    private Integer deliveryFee;

    @ApiModelProperty(notes = "订单中的商品", dataType = "java.util.List")
    private List<OrderProduct> orderProducts = new ArrayList<>();
    @ApiModelProperty(notes = "订单中的操作流水", dataType = "java.util.List")
    private List<OrderFlow> orderFlows;
    @ApiModelProperty(notes = "订单中的套餐", dataType = "java.util.List")
    private List<OrderAssortment> orderAssortment = new ArrayList<>();


    //海鼎重试默认次数
    public static final int DEFAULT_RETRY_COUNT = 3;
    //已重试次数计数器
    private Integer count;
}
