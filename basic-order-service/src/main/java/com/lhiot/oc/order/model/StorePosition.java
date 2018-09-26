package com.lhiot.oc.order.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lhiot.oc.order.model.type.ApplicationType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * Description:门店位置实体类
 *
 * @author yijun
 * @date 2018/09/11
 */
@Data
@ToString(callSuper = true)
@ApiModel
@NoArgsConstructor
public class StorePosition {

    /**
     *
     */
    @JsonProperty("id")
    @ApiModelProperty(value = "ID", dataType = "Long")
    private Long id;

    /**
     * 坐标位置（经度）
     */
    @JsonProperty("lng")
    @ApiModelProperty(value = "坐标位置（经度）", dataType = "Double")
    private Double lng;

    /**
     * 坐标位置（纬度）
     */
    @JsonProperty("lat")
    @ApiModelProperty(value = "坐标位置（纬度）", dataType = "Double")
    private Double lat;

    /**
     * 应用类型
     */
    @JsonProperty("applicationType")
    @ApiModelProperty(value = "应用类型", dataType = "ApplicationType")
    private ApplicationType applicationType;

    /**
     * 门店编号
     */
    @JsonProperty("storeId")
    @ApiModelProperty(value = "门店编号", dataType = "Long")
    private Long storeId;

    private List<Long> storeIds;

}
