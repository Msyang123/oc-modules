package com.lhiot.oc.basic.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lhiot.oc.basic.domain.common.PagerRequestObject;
import com.lhiot.oc.basic.domain.enums.ApplicationType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
* Description:配送订单流程实体类
* @author yijun
* @date 2018/09/16
*/
@Data
@ToString(callSuper = true)
@ApiModel
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DeliverBaseOrder extends PagerRequestObject {

    /**
    *id
    */
    @JsonProperty("id")
    @ApiModelProperty(value = "id", dataType = "Long")
    private Long id;

    /**
    *订单id
    */
    @JsonProperty("orderId")
    @ApiModelProperty(value = "订单id", dataType = "Long")
    private Long orderId;

    /**
    *订单编码
    */
    @JsonProperty("orderCode")
    @ApiModelProperty(value = "订单编码", dataType = "String")
    private String orderCode;

    /**
    *应用类型:APP(视食),WECHAT_MALL(微商城),S_MALL(小程序),F_MALL(鲜果师)
    */
    @JsonProperty("applyType")
    @ApiModelProperty(value = "应用类型:APP(视食),WECHAT_MALL(微商城),S_MALL(小程序),F_MALL(鲜果师)", dataType = "ApplicationType")
    private ApplicationType applyType;

    /**
    *创建时间
    */
    @JsonProperty("createAt")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间", dataType = "Date")
    private java.util.Date createAt;
    

    /**
    *订单用户
    */
    @JsonProperty("userId")
    @ApiModelProperty(value = "订单用户", dataType = "Long")
    private Long userId;

    /**
    *订单总价
    */
    @JsonProperty("totalAmount")
    @ApiModelProperty(value = "订单总价", dataType = "Integer")
    private Integer totalAmount;

    /**
    *实收用户配送费
    */
    @JsonProperty("deliveryFee")
    @ApiModelProperty(value = "实收用户配送费", dataType = "Integer")
    private Integer deliveryFee;

    /**
    *优惠金额
    */
    @JsonProperty("couponAmount")
    @ApiModelProperty(value = "优惠金额", dataType = "Integer")
    private Integer couponAmount;

    /**
    *应付金额
    */
    @JsonProperty("amountPayable")
    @ApiModelProperty(value = "应付金额", dataType = "Integer")
    private Integer amountPayable;

    /**
    *收货人
    */
    @JsonProperty("receiveUser")
    @ApiModelProperty(value = "收货人", dataType = "String")
    private String receiveUser;

    /**
    *联系方式
    */
    @JsonProperty("contactPhone")
    @ApiModelProperty(value = "联系方式", dataType = "String")
    private String contactPhone;

    /**
    *收货地址
    */
    @JsonProperty("address")
    @ApiModelProperty(value = "收货地址", dataType = "String")
    private String address;

    /**
    *备注
    */
    @JsonProperty("remark")
    @ApiModelProperty(value = "备注", dataType = "String")
    private String remark;

    /**
    *配送时间段
    */
    @JsonProperty("deliverTime")
    @ApiModelProperty(value = "配送时间段", dataType = "String")
    private String deliverTime;

    /**
    *经度
    */
    @JsonProperty("lng")
    @ApiModelProperty(value = "经度", dataType = "Double")
    private Double lng;

    /**
    *纬度
    */
    @JsonProperty("lat")
    @ApiModelProperty(value = "纬度", dataType = "Double")
    private Double lat;

    /**
    *配送时间
    */
    @JsonProperty("deliveryTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value = "配送时间", dataType = "Date")
    private java.util.Date deliveryTime;
    

    /**
    *门店编码
    */
    @JsonProperty("storeCode")
    @ApiModelProperty(value = "门店编码", dataType = "String")
    private String storeCode;

    /**
    *门店名称
    */
    @JsonProperty("storeName")
    @ApiModelProperty(value = "门店名称", dataType = "String")
    private String storeName;


    /**
     *订单编码
     */
    @JsonProperty("hdOrderCode")
    @ApiModelProperty(value = "海鼎订单编码", dataType = "String")
    private String hdOrderCode;

    /**
     *业务回调地址
     */
    @JsonProperty("backUrl")
    @ApiModelProperty(value = "业务回调地址", dataType = "String")
    private String backUrl;
    /**
     *配送订单商品
     */
    @JsonProperty("deliverOrderProductList")
    @ApiModelProperty(value = "配送订单商品", dataType = "List")
    private List<DeliverOrderProduct> deliverOrderProductList;


}
