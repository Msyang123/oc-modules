package com.lhiot.oc.payment.feign.domain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Author zhangfeng created in 2018/9/21 12:12
 **/
@Data
@ApiModel
public class BaseUserResult {
    @ApiModelProperty(notes = "基础用户Id",dataType = "Long")
    private Long id;
    @ApiModelProperty(notes = "基础用户手机号码",dataType = "String")
    private String phone;
    @ApiModelProperty(notes = "用户余额",dataType = "Long")
    private Long balance;
    @ApiModelProperty(notes = "用户是否锁定",dataType = "LockStatus")
    private LockStatus locked;
    @ApiModelProperty(notes = "用户积分",dataType = "Long")
    private Long memberPoints;
    @ApiModelProperty(notes = "用户真实姓名",dataType = "String")
    private String realName;
    @ApiModelProperty(notes = "身份证号码",dataType = "String")
    private String idCard;
}
