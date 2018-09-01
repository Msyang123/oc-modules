package com.lhiot.oc.order.model;

import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * @author Leon (234239150@qq.com) created in 9:55 18.9.1
 */
@Data
@ApiModel
public class RefundParam {
    private long orderId;
    private long[] goodsIds;
}
