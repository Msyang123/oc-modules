package com.lhiot.oc.basic.feign.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel
@Data
public class ProductsStandard {

    @ApiModelProperty(notes = "商品规格id", dataType = "Long")
    private Long id;

    @ApiModelProperty(notes = "商品id", dataType = "Long")
    private Long productId;

    @ApiModelProperty(notes = "商品条码", dataType = "String")
    private String barcode;

    @ApiModelProperty(notes = "商品规格", dataType = "String")
    private String specification;

    @ApiModelProperty(notes = "商品数量或重量", dataType = "Double")
    private Double standardQty;


    @ApiModelProperty(notes = "上下架状态 0-下架 1-上架", dataType = "String")
    private String shelvesStatus;

    @ApiModelProperty(notes = "商品价格", dataType = "String")
    private Integer price;

    @ApiModelProperty(notes = "商品出售价格", dataType = "String")
    private Integer salePrice;

    @ApiModelProperty(notes = "商品排序", dataType = "String")
    private String rank;

    @ApiModelProperty(notes = "规格描述", dataType = "String")
    private String description;

    @ApiModelProperty(notes = "商品名称", dataType = "String")
    private String productName;

    @ApiModelProperty(notes = "商品单位", dataType = "String")
    private String productUnit;

    @ApiModelProperty(notes = "商品编码", dataType = "String")
    private String productCode;

    @ApiModelProperty(notes = "商品分类名称", dataType = "String")
    private String groupName;

    @ApiModelProperty(notes = "基础价格,用于天降水果活动", dataType = "String")
    private Integer unitPrice;

    @ApiModelProperty(notes = "基础单位", dataType = "String")
    private String baseUnit;

    @ApiModelProperty(notes = "商品小图标", dataType = "String")
    private String smallImage;
    
    @ApiModelProperty(notes = "商品大图标", dataType = "String")
    private String largeImage;
    
    @ApiModelProperty(notes = "商品图片", dataType = "String")
    private String image;

    @ApiModelProperty(notes = "套餐中，该规格商品的份数", dataType = "Integer")
    private Integer relationCount;
    
    @ApiModelProperty(notes = "允许销售的最低库存", dataType = "Integer")
    private Integer limitQty;
    
    @JsonIgnore
    @ApiModelProperty(notes = "订单所在的套餐名称", dataType = "String")
    private String assortmentName;
    
    @JsonIgnore
    @ApiModelProperty(notes = "订单套餐id", dataType = "Long")
    private Long assortmentId;
    /**
     *基础重量，比如1个0.2kg填写0.2
     */
    @ApiModelProperty(value = "基础重量，比如1个0.2kg填写0.2", dataType = "Double")
    private Double baseQty;

}
