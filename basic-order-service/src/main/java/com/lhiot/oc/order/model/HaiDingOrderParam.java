package com.lhiot.oc.order.model;

import com.lhiot.oc.order.entity.OrderProduct;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

/**
 * @author zhangfeng created in 2018/9/29 11:35
 **/
@Data
@ApiModel
public class HaiDingOrderParam {
    private Long id;
    private String code;
    private Long userId;
    private String applyType;
    private String orderType;
    private Long storeId;
    private String storeCode;
    private String storeName;
    private String receivingWay;
    private Integer totalAmount;
    private Integer amountPayable;
    private Integer deliveryAmount;
    private Integer couponAmount;
    private String orderStatus;
    @ApiModelProperty(notes = "收货地址：门店自提订单填写门店地址", dataType = "String")
    private String address;

    @ApiModelProperty(notes = "收货人", dataType = "String")
    private String receiveUser;
    @ApiModelProperty(notes = "收货人联系方式", dataType = "String")
    private String contactPhone;
    private String remark;
    @ApiModelProperty(notes = "提货截止时间", dataType = "String")
    private Timestamp deliveryEndTime;

    private String returnReason;
    private String hdOrderCode;
    private Date payAt;

    @ApiModelProperty(notes = "收货地址：门店自提订单填写门店地址", dataType = "OrderProduct")
    private List<OrderProduct> orderProducts;

}
