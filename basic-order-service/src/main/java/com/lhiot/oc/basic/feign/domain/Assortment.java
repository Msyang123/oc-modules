package com.lhiot.oc.basic.feign.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

/**
* 描述：商品套餐
* @author yijun
* @date 2018-07-21
*/
@Data
@ApiModel
public class Assortment implements Serializable {

	@JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(notes = "id", dataType = "Long")
    private Long id;

    @ApiModelProperty(notes = "套餐名称", dataType = "String")
    private String assortmentName;

    @ApiModelProperty(notes = "当前状态(有效- VALID 无效-INVALID)", dataType = "String")
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(notes = "创建时间", dataType = "Timestamp")
    private Timestamp createTime;

    @ApiModelProperty(notes = "创建人", dataType = "String")
    private String createUser;

    @ApiModelProperty(notes = "套餐图片", dataType = "String")
    private String assortmentImage;

    @ApiModelProperty(notes = "套餐详情", dataType = "String")
    private String detail;

    @ApiModelProperty(notes = "套餐简介", dataType = "String")
    private String introduce;

    @ApiModelProperty(notes = "套餐价格", dataType = "Integer")
    private Integer price;
    
    @ApiModelProperty(notes = "套餐商品", dataType = "java.util.List")
    private List<ProductsStandard> assortmentProducts;
}
