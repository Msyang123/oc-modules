package com.lhiot.oc.order.model;

import com.lhiot.oc.order.feign.DeliverTime;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author zhangfeng create in 9:36 2018/12/5
 */
@Data
public class DeliverParam {

    @ApiModelProperty(notes = "应用类型",dataType = "String")
    private String applicationType;

    @ApiModelProperty(value = "配送类型",dataType = "String")
    private String deliveryType;

    @ApiModelProperty(value = "坐标系", dataType = "String")
    private String coordinate;

    @ApiModelProperty(value = "业务回调地址", dataType = "String")
    private String backUrl;

    @ApiModelProperty(value = "经度", dataType = "Double")
    private Double lng;
    @ApiModelProperty(value = "纬度", dataType = "Double")
    private Double lat;

    @ApiModelProperty(value = "配送时间段", dataType = "String")
    private DeliverTime deliverTime;
}
