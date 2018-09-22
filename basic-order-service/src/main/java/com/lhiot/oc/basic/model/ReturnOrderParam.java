package com.lhiot.oc.basic.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel
/**
 * 退款商品参数
 */
public class ReturnOrderParam {

    @ApiModelProperty(notes = "退货原因", dataType = "String")
    private String returnReason;

    @ApiModelProperty(notes = "订单商品ID（一个或者多个）多个以,分割", dataType = "String")
    private String orderProductIds;
}
