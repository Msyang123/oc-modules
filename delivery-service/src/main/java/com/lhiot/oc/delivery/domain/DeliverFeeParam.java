package com.lhiot.oc.delivery.domain;

import com.lhiot.oc.delivery.domain.enums.ApplicationType;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
/**
 * 配送费计算传入参数
 */
public class DeliverFeeParam {

    @ApiModelProperty(notes = "订单费用",dataType = "Integer")
    private Integer orderFee;
    @ApiModelProperty(notes = "订单所在门店",dataType = "Long")
    private Long storeId;

    @ApiModelProperty(notes = "订单重量(kg)",dataType = "Double")
    private Double weight;

    @ApiModelProperty(notes = "应用类型",dataType = "ApplicationType")
    private ApplicationType applicationType;

    @ApiModelProperty(notes = "订单目标坐标位置-经度",dataType = "Double")
    private Double targetLng;
    @ApiModelProperty(notes = "订单目标坐标位置-纬度",dataType = "Double")
    private Double targetLat;
    @ApiModelProperty(notes = "配送时间 json格式如 {\"display\":\"立即配送\",\"startTime\":\"2018-08-15 11:30:00\",\"endTime\":\"2018-08-15 12:30:00\"}",dataType = "String")
    private String deliveryTime;
}
