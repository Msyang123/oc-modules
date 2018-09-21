package com.lhiot.oc.payment.feign.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.lhiot.oc.payment.domain.enums.ApplicationType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
@ApiModel
public class UserDetailResult {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty(notes = "生日", dataType = "String")
    private String birthday;

    @ApiModelProperty(notes = "性别:man(男),women(女),privary(保密)", dataType = "String")
    private String sex;

    @ApiModelProperty(notes = "手机号", dataType = "String")
    private String phone;

    @ApiModelProperty(notes = "真名", dataType = "String")
    private String realName;

    @ApiModelProperty(notes = "昵称", dataType = "String")
    private String nickname;

    @ApiModelProperty(notes = "邮箱", dataType = "String")
    private String email;

    @ApiModelProperty(notes = "QQ", dataType = "String")
    private String qq;

    @ApiModelProperty(notes = "头像", dataType = "String")
    private String avatar;

    @ApiModelProperty(notes = "地址", dataType = "String")
    private String address;

    @ApiModelProperty(notes = "备注", dataType = "String")
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(notes = "注册时间", dataType = "Date")
    private Date registerAt;

    @ApiModelProperty(notes = "积分", dataType = "Integer")
    private Long point = 0L;

    @ApiModelProperty(notes = "鲜果币", dataType = "Long")
    private Long balance = 0L;

    @ApiModelProperty(notes = "微信开放平台单一应用唯一识别openId", dataType = "String")
    private String openId;

    @ApiModelProperty(notes = "同一个微信开放平台多个应用唯一识别unionId", dataType = "String")
    private String unionId;

    @ApiModelProperty(notes = "t_base_user的id", dataType = "Long")
    private Long baseUserId;

    @ApiModelProperty(notes = "是否锁定0未锁定1锁定", dataType = "LockStatus")
    private LockStatus locked;
    /**
     *应用类型ID
     */
    @JsonProperty("applicationType")
    @ApiModelProperty(value = "应用类型ID", dataType = "ApplicationType")
    private ApplicationType applicationType;

    @ApiModelProperty(notes = "免密支付权限", dataType = "SwitchStatus")
    private SwitchStatus paymentPermissions;
}
