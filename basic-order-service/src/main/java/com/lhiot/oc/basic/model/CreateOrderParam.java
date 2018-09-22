package com.lhiot.oc.basic.model;

import com.leon.microx.util.BeanUtils;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Data
@ApiModel
public class CreateOrderParam {

    private Long userId;
    private ApplicationType applicationType;
    private Long storeId;
    private ReceivingWay receivingWay;
    private Integer couponAmount = 0;
    private Integer totalAmount;
    @ApiModelProperty(notes = "配送费", dataType = "Integer")
    private Integer deliveryAmount = 0;
    private Integer amountPayable = 0;
    @ApiModelProperty(notes = "收货地址：门店自提订单填写收货地址", dataType = "String")
    private String address;
    @ApiModelProperty(notes = "收货人", dataType = "String")
    private String receiveUser;
    @ApiModelProperty(notes = "收货人昵称", dataType = "String")
    private String nickname;
    @ApiModelProperty(notes = "收货人联系方式", dataType = "String")
    private String contactPhone;
    private String remark;
    @ApiModelProperty(notes = "提货截止时间", dataType = "String")
    private Timestamp deliveryEndTime;
    @ApiModelProperty(notes = "商品列表", dataType = "java.util.List")
    private List<OrderProductParam> orderProducts;
    @ApiModelProperty(notes = "配送时间 json格式如 {\"display\":\"立即配送\",\"startTime\":\"2018-08-15 11:30:00\",\"endTime\":\"2018-08-15 12:30:00\"}", dataType = "String")
    private String deliveryTime;

    /**
     * 对象属性复制
     *
     * @return
     */
    public BaseOrderInfo toOrderObject() {
        BaseOrderInfo baseOrderInfo = new BaseOrderInfo();
        BeanUtils.of(baseOrderInfo).populate(BeanUtils.of(this).toMap());
        return baseOrderInfo;
    }
}
