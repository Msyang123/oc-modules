package com.lhiot.oc.basic.feign.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.ToString;

/**
 * 取消订单对应的 字段
 */
@ToString
public class ElemeCancelOrderRequest extends AbstractRequest {


    @Data
    @ToString
    public static class ElemeCancelOrderRequstData extends AbstractRequestData{
        @JsonProperty("order_cancel_reason_code")
        private int orderCancelReasonCode;
        @JsonProperty("order_cancel_code")
        private Integer orderCancelCode;
        @JsonProperty("order_cancel_description")
        private String orderCancelDescription;
        @JsonProperty("order_cancel_time")
        @JsonSerialize(using = ToStringSerializer.class)
        private long orderCancelTime;
        @JsonProperty("order_cancel_notify_url")
        private String orderCancelNotifyUrl;

    }
}
