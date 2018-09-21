package com.lhiot.oc.payment.domain.enums;

import lombok.Getter;

/**
 * 普通队列配置
 */
public enum  NormalExchange {
    /**
     * 订单状态流转
     */
    //ORDER_STATUS_FLOW("s-mall-order", "order-status-flow"),
    SEND_TO_HD("hd-exchange","order-send-hd");//发送海鼎主题队列
	//SEND_TO_DADA("dada-exchange","order-send-dada");
    @Getter
    String exchangeName;

    @Getter
    String queueName;

    NormalExchange(String name, String queue) {
        this.exchangeName = name;
        this.queueName = queue;
    }
}
