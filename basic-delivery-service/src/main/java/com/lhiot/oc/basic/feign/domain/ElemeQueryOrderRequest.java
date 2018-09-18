package com.lhiot.oc.basic.feign.domain;

import lombok.Data;
import lombok.ToString;

/**
 * 查询订单对应字段
 */
@ToString
public class ElemeQueryOrderRequest extends AbstractRequest {

    @Data
    @ToString
    public static class ElemeQueryRequestData extends AbstractRequestData{

    }
}
