package com.lhiot.oc.basic.domain;

import com.lhiot.oc.basic.domain.enums.ApplicationTypeEnum;
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
    @ApiModelProperty(notes = "订单费用",dataType = "Long")
    private Long storeId;

    @ApiModelProperty(notes = "订单重量(kg)",dataType = "Double")
    private Double weight;

    @ApiModelProperty(notes = "应用类型",dataType = "ApplicationTypeEnum")
    private ApplicationTypeEnum applicationType;

    @ApiModelProperty(notes = "订单目标坐标位置-经度",dataType = "Double")
    private Double targetCoordy;
    @ApiModelProperty(notes = "订单目标坐标位置-纬度",dataType = "Double")
    private Double targetCoordx;
    @ApiModelProperty(notes = "配送时间 json格式如 {\"display\":\"立即配送\",\"startTime\":\"2018-08-15 11:30:00\",\"endTime\":\"2018-08-15 12:30:00\"}",dataType = "String")
    private String deliveryTime;
}
