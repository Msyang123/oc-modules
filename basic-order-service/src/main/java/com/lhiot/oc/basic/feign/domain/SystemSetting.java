package com.lhiot.oc.basic.feign.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lhiot.order.domain.common.PagerRequestObject;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
* Description:系统设置参数实体类
* @author yijun
* @date 2018/07/24
*/
@Data
@ToString(callSuper = true)
@ApiModel
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SystemSetting extends PagerRequestObject{

    /**
    *id
    */
    @JsonProperty("id")
    @ApiModelProperty(value = "id", dataType = "Long")
    private Long id;

    /**
    *系统设置key
    */
    @JsonProperty("indexName")
    @ApiModelProperty(value = "系统设置key", dataType = "String")
    private String indexName;

    /**
    *系统设置value
    */
    @JsonProperty("indexValue")
    @ApiModelProperty(value = "系统设置value", dataType = "String")
    private String indexValue;

    /**
    *描述
    */
    @JsonProperty("description")
    @ApiModelProperty(value = "描述", dataType = "String")
    private String description;

    /**
    *分类编号
    */
    @JsonProperty("categoryId")
    @ApiModelProperty(value = "分类编号", dataType = "Long")
    private Long categoryId;

}
