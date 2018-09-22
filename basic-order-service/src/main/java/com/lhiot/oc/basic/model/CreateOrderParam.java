package com.lhiot.oc.basic.model;

import com.leon.microx.util.BeanUtils;
import com.lhiot.oc.basic.model.type.ApplicationType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@ApiModel
public class CreateOrderParam {

    private Long userId;
    private ApplicationType applicationType;
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
    private Date deliveryEndTime;
    @ApiModelProperty(notes = "配送时间 json格式如 {\"display\":\"立即配送\",\"startTime\":\"2018-08-15 11:30:00\",\"endTime\":\"2018-08-15 12:30:00\"}", dataType = "String")
    private String deliveryTime;
    @ApiModelProperty(notes = "商品列表", dataType = "OrderProduct")
    private List<OrderProduct> orderProducts;
    @ApiModelProperty(notes = "门店信息", dataType = "OrderStoreParam")
    private OrderStore orderStore;

    /**
     * 对象属性复制
     *
     * @return
     */
    public BaseOrder toOrderObject() {
        BaseOrder baseOrder = new BaseOrder();
        BeanUtils.of(baseOrder).populate(BeanUtils.of(this).toMap());
        return baseOrder;
    }
}
