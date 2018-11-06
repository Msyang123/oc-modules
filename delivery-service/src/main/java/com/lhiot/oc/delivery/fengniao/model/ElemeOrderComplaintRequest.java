package com.lhiot.oc.delivery.fengniao.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 投诉订单 对应的 字段
 */

@ToString
public class ElemeOrderComplaintRequest extends AbstractRequest {

	@Data
	@ToString
	@EqualsAndHashCode(callSuper = true)
	public static class ElemeOrderComplaintRequstData extends AbstractRequestData{

		@JsonProperty("order_complaint_code")
		private Integer orderComplaintCode;
		@JsonProperty("order_complaint_desc")
		private String orderComplaintDesc;
		@JsonProperty("order_complaint_time")
		@JsonSerialize(using = ToStringSerializer.class)
		private Long orderComplaintTime;
	}

}
