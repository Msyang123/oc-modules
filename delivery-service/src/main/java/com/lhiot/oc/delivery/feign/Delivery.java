package com.lhiot.oc.delivery.feign;

import com.lhiot.oc.delivery.model.DeliverType;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author zhangfeng create in 9:05 2019/1/21
 */
@Data
public class Delivery {
    @ApiModelProperty(notes = "海鼎订单编号",dataType = "String")
    private String orderId;

    @ApiModelProperty(notes = "配送单Id",dataType = "String")
    private String deliverId;

    @ApiModelProperty(notes = "配送类型",dataType = "DeliverType")
    private DeliverType deliverType;

    @ApiModelProperty(notes = "配送员名称",dataType = "String")
    private String deliverName;

    @ApiModelProperty(notes = "配送员联系方式",dataType = "String")
    private String deliverPhone;
}
