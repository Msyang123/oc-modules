package com.lhiot.oc.basic.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lhiot.order.domain.common.PagerRequestObject;
import com.lhiot.order.domain.enums.RefundStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;


@EqualsAndHashCode(callSuper = false)
@Data
@ApiModel
public class OrderProduct extends PagerRequestObject {

    private Long id;
    private Long orderId;

    @ApiModelProperty(notes = "商品规格id",dataType = "Long")
    private Long standardId;
    @ApiModelProperty(notes = "商品条码",dataType = "String")
    private String barcode;
    @ApiModelProperty(notes = "购买价格",dataType = "Integer")
    private Integer price;
    @ApiModelProperty(notes = "规格价格",dataType = "Integer")
    private Integer standardPrice;
    @ApiModelProperty(notes = "购买份数",dataType = "Integer")
    private Integer productQty;
    @ApiModelProperty(notes = "商品数量或者重量(规格表对应的数量或者重量)",dataType = "Integer")
    private Double standardQty;
    @ApiModelProperty(notes = "是否退货",dataType = "RefundStatus")
    private RefundStatus refundStatus;
    @ApiModelProperty(notes = "除去优惠金额后单个商品的价格",dataType = "Integer")
    private Integer discountPrice;
    @ApiModelProperty(notes = "套餐id",dataType = "Long")
    private Long assortmentId;

    @ApiModelProperty(notes = "商品名称",dataType = "String")
    private String productName;
    @ApiModelProperty(notes = "商品主图",dataType = "String")
    private String image;
    @ApiModelProperty(notes = "商品小图标",dataType = "String")
    private String smallImage;
    @ApiModelProperty(notes = "大图标",dataType = "String")
    private String largeImage;

    
    @JsonIgnore
    @ApiModelProperty(notes = "允许销售的最低库存，用来检测套餐中商品库存",dataType = "Long")
    private Integer limitQty;
    
}
