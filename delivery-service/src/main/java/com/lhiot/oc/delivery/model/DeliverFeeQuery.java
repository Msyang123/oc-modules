package com.lhiot.oc.delivery.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.ToString;

/**
 * @author Leon (234239150@qq.com) created in 15:36 18.11.10
 */
@Data
@ApiModel
@ToString
public class DeliverFeeQuery {

    @ApiModelProperty(notes = "订单费用", dataType = "Integer")
    private Integer orderFee;
    @ApiModelProperty(notes = "订单所在门店", dataType = "Long")
    private Long storeId;

    @ApiModelProperty(notes = "订单重量(kg)", dataType = "Double")
    private Double weight;

    @ApiModelProperty(notes = "应用类型", dataType = "ApplicationType")
    private String applicationType;

    @ApiModelProperty(notes = "订单目标坐标位置-经度", dataType = "Double")
    private Double targetLng;

    @ApiModelProperty(notes = "订单目标坐标位置-纬度", dataType = "Double")
    private Double targetLat;

    @ApiModelProperty(notes = "目标坐标位置使用的坐标系", dataType = "CoordinateSystem")
    private CoordinateSystem coordinateSystem;

    @ApiModelProperty(notes = "配送时间", dataType = "DeliverTime", example = "{\"display\":\"立即配送\",\"startTime\":\"2018-08-15 11:30:00\",\"endTime\":\"2018-08-15 12:30:00\"}")
    private DeliverTime deliveryTime;
}
