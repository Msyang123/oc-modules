package com.lhiot.oc.order.model;

import com.lhiot.oc.order.model.type.RefundType;
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
    private String reason;

    @ApiModelProperty(notes = "退货类型:ALL-全部退货,PART-部分退货",dataType = "RefundType")
    private RefundType refundType;

    @ApiModelProperty(notes = "订单商品主键ID（一个或者多个）多个以,分割", dataType = "String")
    private String orderProductIds;
}
