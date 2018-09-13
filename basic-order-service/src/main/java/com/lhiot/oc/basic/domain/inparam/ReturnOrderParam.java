package com.lhiot.oc.basic.domain.inparam;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@ApiModel
public class ReturnOrderParam {

    @ApiModelProperty(notes = "用户ID", dataType = "Long")
    @Min(1)
    @NotNull
    private Long userId;

    @ApiModelProperty(notes = "订单ID", dataType = "Long")
    @Min(1)
    @NotNull
    private Long orderId;

    @ApiModelProperty(notes = "商品规格ID（一个或者多个）多个以,分割", dataType = "String")
    private String orderBarcodeIds;

    @ApiModelProperty(notes = "套餐id（一个或者多个）多个以,分割", dataType = "String")
    private String assortmentIds;

    @ApiModelProperty(notes = "退货原因", dataType = "String")
    private String returnReason;

    @ApiModelProperty(notes = "商品规格ID（一个或者多个）多个以,分割", dataType = "String")
    private String orderProductIds;
}
