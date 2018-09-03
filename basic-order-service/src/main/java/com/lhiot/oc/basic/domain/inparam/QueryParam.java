package com.lhiot.oc.basic.domain.inparam;

import com.lhiot.oc.basic.domain.common.PagerRequestObject;
import com.lhiot.oc.basic.domain.enums.Apply;
import com.lhiot.oc.basic.domain.enums.HdStatus;
import com.lhiot.oc.basic.domain.enums.OrderStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;


/**
 * Description:订单查询实体类
 * @author Limiaojun
 * @date 2018/06/04
 */
@Data
@ToString(callSuper = true)
@ApiModel
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QueryParam extends PagerRequestObject {
    @ApiModelProperty("订单code")
    private String orderCode;

    @ApiModelProperty("手机号")
    private String phone;

    @ApiModelProperty("开始时间")
    private String startTime;

    @ApiModelProperty("结束时间")
    private String endTime;

    @ApiModelProperty("订单状态")
    private OrderStatus status;

    @ApiModelProperty("海鼎状态")
    private HdStatus hdStatus;

    @ApiModelProperty("订单id")
    private Long storeId;

    @ApiModelProperty("用户id")
    private Long userId;

    @ApiModelProperty("用户ids ','分割")
    private String userIds;

    private String[] userIdList;

    @ApiModelProperty("应用类型")
    private Apply applyType;
}
