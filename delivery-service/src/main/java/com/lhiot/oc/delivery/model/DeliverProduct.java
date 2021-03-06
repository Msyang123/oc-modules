package com.lhiot.oc.delivery.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.ToString;

/**
 * 商品Model
 * @author Leon (234239150@qq.com) created in 8:59 18.11.11
 */
@Data
@ApiModel
@ToString
public class DeliverProduct {


    @ApiModelProperty(value = "购买价格", dataType = "Integer")
    private Integer price;

    @ApiModelProperty(value = "购买份数", dataType = "Integer")
    private Integer productQty;

    @ApiModelProperty(value = "单个商品总重量)", dataType = "Double")
    private Double totalWeight;

    @ApiModelProperty(value = "除去优惠金额后单个商品的价格", dataType = "Integer")
    private Integer discountPrice;

    @ApiModelProperty(value = "商品名称", dataType = "String")
    private String productName;

}
