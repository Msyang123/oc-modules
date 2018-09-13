package com.lhiot.oc.basic.feign.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AbstractRequestData {
    @JsonProperty("partner_order_code")
    private String partnerOrderCode;
}
