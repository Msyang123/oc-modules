package com.lhiot.oc.basic.domain.inparam;

import com.leon.microx.util.BeanUtils;
import com.lhiot.oc.basic.domain.BaseOrderInfo;
import com.lhiot.oc.basic.domain.enums.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Data
@ApiModel
public class CreateOrderParam {

    private Long userId;
    private Apply apply;
    private OrderType orderType;
    private Long storeId;
    private String storeCode;
    private String storeName;
    private ReceivingWay receivingWay;
    private Integer couponAmount;
    private Integer totalAmount;
    private Integer amountPayable;
    @ApiModelProperty(notes = "收货地址：门店自提订单填写收货地址",dataType = "String")
    private  String  address;

    @ApiModelProperty(notes = "收货人",dataType = "String")
    private String receiveUser;

    @ApiModelProperty(notes = "收货人昵称",dataType = "String")
    private String nickname;
    @ApiModelProperty(notes = "收货人联系方式",dataType = "String")
    private String contactPhone;
    private String remark;
    @ApiModelProperty(notes = "提货截止时间",dataType = "String")
    private Timestamp  deliveryEndTime;
    @ApiModelProperty(notes = "商品列表",dataType = "java.util.List")
    private List<OrderProductParam> orderProducts;
    @ApiModelProperty(notes = "套餐列表",dataType = "java.util.List")
    private List<OrderAssortmentParam> assortments;
    @ApiModelProperty(notes = "优惠券id",dataType = "Long")
    private Long couponId;
    @ApiModelProperty(notes = "配送费",dataType = "Integer")
    private Integer deliveryAmount;
    @ApiModelProperty(notes = "坐标位置-经度",dataType = "Double")
    private Double storeCoordy;
    @ApiModelProperty(notes = "坐标位置-纬度",dataType = "Double")
    private Double storeCoordx;
    @ApiModelProperty(notes = "配送时间 json格式如 {\"display\":\"立即配送\",\"startTime\":\"2018-08-15 11:30:00\",\"endTime\":\"2018-08-15 12:30:00\"}",dataType = "String")
    private String deliveryTime;
    /**
     * 对象属性复制
     * @return
     */
    public BaseOrderInfo toOrderObject(){
        BaseOrderInfo baseOrderInfo = new BaseOrderInfo();
        BeanUtils.of(baseOrderInfo).populate(BeanUtils.of(this).toMap());
        return baseOrderInfo;
    }

    /**
     * 订单商品
     */
    @Data
    @ApiModel
    public static class OrderProductParam {

        private Long standardId;
        private Integer price;
        @ApiModelProperty(notes = "购买份数", dataType = "Integer")
        private Integer buyCount;
    }

    /**
     * 订单套餐
     */
    @Data
    @ApiModel
    public static class OrderAssortmentParam {

        private Long assortmentId;
        private Integer price;
        @ApiModelProperty(notes = "购买份数", dataType = "Integer")
        private Integer buyCount;
    }

}
