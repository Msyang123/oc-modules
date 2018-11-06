package com.lhiot.oc.delivery.meituan.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 订单id信息
 */
@Data
public class OrderIdInfo {
	/**
     * 配送唯一标识
     */
    @JsonProperty("mt_peisong_id")
    private String mtPeisongId;
	/**
     * 订单ID
     */
    @JsonProperty("order_id")
    private String orderId;
	/**
     * 配送活动标识
     */
    @JsonProperty("delivery_id")
    private long deliveryId;
	/**
     * 目的地id
     */
    @JsonProperty("destination_id")
    private String destinationId;
	/**
     * 订单配送距离
     */
    @JsonProperty("delivery_distance")
    private Integer deliveryDistance;
	/**
     * 订单配送价格（面向商家）
     */
    @JsonProperty("delivery_fee")
    private Double deliveryFee;
	/**
     * 路区信息
     */
    @JsonProperty("road_area")
    private String roadArea;

    @Override
    public String toString() {
        return "{" +
                "\"mt_peisong_id\":\"" + mtPeisongId +
                "\", \"order_id=\":\"" + orderId +
                "\", \"delivery_id\":" + deliveryId +
                ", \"destination_id=\":\"" + destinationId +
                "\", \"delivery_distance\":" + deliveryDistance +
                ", \"delivery_fee\":" + deliveryFee +
                ", \"road_area\":\"" + roadArea + "\""+
                "}";
    }
}
