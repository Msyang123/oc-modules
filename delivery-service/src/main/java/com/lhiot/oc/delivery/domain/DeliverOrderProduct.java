package com.lhiot.oc.delivery.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lhiot.oc.delivery.domain.common.PagerRequestObject;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
* Description:配送订单商品列实体类
* @author yijun
* @date 2018/09/18
*/
@Data
@ToString(callSuper = true)
@ApiModel
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DeliverOrderProduct extends PagerRequestObject {

    /**
    *配送订单商品id
    */
    @JsonProperty("id")
    @ApiModelProperty(value = "配送订单商品id", dataType = "Long")
    private Long id;

    /**
    *配送订单id
    */
    @JsonProperty("deliverBaseOrderId")
    @ApiModelProperty(value = "配送订单id", dataType = "Long")
    private Long deliverBaseOrderId;

    /**
    *商品条码
    */
    @JsonProperty("barcode")
    @ApiModelProperty(value = "商品条码", dataType = "String")
    private String barcode;

    /**
    *购买价格
    */
    @JsonProperty("price")
    @ApiModelProperty(value = "购买价格", dataType = "Integer")
    private Integer price;

    /**
    *规格价格
    */
    @JsonProperty("standardPrice")
    @ApiModelProperty(value = "规格价格", dataType = "Integer")
    private Integer standardPrice;

    /**
    *购买份数
    */
    @JsonProperty("productQty")
    @ApiModelProperty(value = "购买份数", dataType = "Integer")
    private Integer productQty;

    /**
    *商品数量或者重量(规格表对应的数量或者重量)
    */
    @JsonProperty("standardQty")
    @ApiModelProperty(value = "商品数量或者重量(规格表对应的数量或者重量)", dataType = "Double")
    private Double standardQty;

    /**
     *商品基础重量)
     */
    @JsonProperty("baseWeight")
    @ApiModelProperty(value = "商品基础重量)", dataType = "Double")
    private Double baseWeight;

    /**
    *除去优惠金额后单个商品的价格
    */
    @JsonProperty("discountPrice")
    @ApiModelProperty(value = "除去优惠金额后单个商品的价格", dataType = "Integer")
    private Integer discountPrice;

    /**
    *商品名称
    */
    @JsonProperty("productName")
    @ApiModelProperty(value = "商品名称", dataType = "String")
    private String productName;

    /**
    *商品主图
    */
    @JsonProperty("image")
    @ApiModelProperty(value = "商品主图", dataType = "String")
    private String image;

    /**
    *商品小图标
    */
    @JsonProperty("smallImage")
    @ApiModelProperty(value = "商品小图标", dataType = "String")
    private String smallImage;

    /**
    *大图标
    */
    @JsonProperty("largeImage")
    @ApiModelProperty(value = "大图标", dataType = "String")
    private String largeImage;

}
