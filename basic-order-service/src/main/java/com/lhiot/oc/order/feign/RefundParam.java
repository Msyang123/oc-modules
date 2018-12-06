package com.lhiot.oc.order.feign;

import lombok.Data;

/**
 * @author zhangfeng create in 12:18 2018/12/6
 */
@Data
public class RefundParam {
    private Integer fee;

    private String notifyUrl;

    private String reason;
}
