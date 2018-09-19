package com.lhiot.oc.delivery.fengniao.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 查询订单对应字段
 */
@ToString
public class ElemeQueryOrderRequest extends AbstractRequest {

    @Data
    @ToString
    @EqualsAndHashCode(callSuper = true)
    public static class ElemeQueryRequestData extends AbstractRequestData{

    }
}
