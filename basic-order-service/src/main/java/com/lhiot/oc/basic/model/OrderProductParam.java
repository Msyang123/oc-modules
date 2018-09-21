package com.lhiot.oc.basic.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Author zhangfeng created in 2018/9/19 9:20
 **/
@Data
@ApiModel
public class OrderProductParam {
    @ApiModelProperty(notes = "商品上架Id")
    private Long shelfId;
    @ApiModelProperty(notes = "商品购买价格")
    private Integer price;
    @ApiModelProperty(notes = "购买份数", dataType = "Integer")
    private Integer buyCount;
}
