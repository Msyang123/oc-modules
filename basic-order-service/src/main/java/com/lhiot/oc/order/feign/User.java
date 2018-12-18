package com.lhiot.oc.order.feign;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author zhangfeng create in 16:55 2018/12/18
 */
@Data
public class User {

    @ApiModelProperty(notes = "手机号", dataType = "String")
    private String phone;

    @ApiModelProperty(notes = "昵称", dataType = "String")
    private String nickname;
}
