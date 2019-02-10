package com.lhiot.oc.delivery.client.meituan.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by gw on 2018/1/26.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EvaluateRequest extends AbstractRequest {
	/**
	 * 配送活动标识
	 */
	private Long deliveryId;
	/**
	 * 配送唯一标识
	 */
	private String mtPeisongId;
	/**
	 * 评分（5分制）
	 * 预留参数，不作为骑手反馈参考
	 * 合作方需传入0-5之间分数或者不传，否则报错
	 */
	private Integer score;
	/**
	 * 评论内容（评论的字符长度需小于1024）
	 */
	private String commentContent;


	@Override
	public String toString() {
		return "EvaluateRequest{" +
				"deliveryId=" + deliveryId +
				", mtPeisongId='" + mtPeisongId + '\'' +
				", score=" + score +
				", commentContent='" + commentContent + '\'' +
				'}';
	}
}
