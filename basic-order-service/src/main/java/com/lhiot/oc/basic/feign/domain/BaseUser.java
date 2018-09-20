package com.lhiot.oc.basic.feign.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lhiot.oc.basic.domain.common.PagerRequestObject;
import com.lhiot.oc.basic.domain.enums.ApplicationType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
* Description:公共用户实体类
* @author yijun
* @date 2018/07/24
*/
@Data
@ToString(callSuper = true)
@ApiModel
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BaseUser extends PagerRequestObject {

    /**
    *
    */
    @JsonProperty("id")
    @ApiModelProperty(value = "", dataType = "Long")
    private Long id;

    /**
    *手机号码
    */
    @JsonProperty("phone")
    @ApiModelProperty(value = "手机号码", dataType = "String")
    private String phone;

    /**
    *鲜果币
    */
    @JsonProperty("currency")
    @ApiModelProperty(value = "鲜果币", dataType = "Integer")
    private Integer currency;

    /**
    *会员积分
    */
    @JsonProperty("memberPoints")
    @ApiModelProperty(value = "会员积分", dataType = "Integer")
    private Integer memberPoints;

    /**
    *是否锁定 UNLOCK-未锁定 LOCK-锁定
    */
    @JsonProperty("locked")
    @ApiModelProperty(value = "是否锁定 UNLOCK-未锁定 LOCK-锁定", dataType = "String")
    private String locked;

    /**
    *应用类型ID
    */
    @JsonProperty("applicationType")
    @ApiModelProperty(value = "应用类型ID", dataType = "ApplicationType")
    private ApplicationType applicationType;

    /**
     *unionId
     */
    @JsonProperty("unionId")
    @ApiModelProperty(value = "unionId", dataType = "String")
    private String unionId;

}
