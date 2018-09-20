package com.lhiot.oc.delivery.domain.enums;

import lombok.Getter;

/**
 * 延迟队列配置
 */
public enum  DelayExchange {
    /**
     * name 表示信道名称 queues表示绑定在该信道下的死性队列以及转发队列
     */
    ORDER_DELAY("order-delay-exchange",new DelayQueues[]{DelayQueues.ORDER_TIMEOUT}),//订单超时取消
    ORDER_FINISHED_DELAY("order--finished-delay-exchange",new DelayQueues[]{DelayQueues.ORDER_FINISHED_TIMEOUT}),//订单自动完成
    ORDER_NOTICE_DELAY("order-notice-delay-exchange",new DelayQueues[]{DelayQueues.ORDER_NOTICE_TIMEOUT}),//门店自提订单48小时超时发送短信
    ORDER_REFUND_DELAY("order-refund-delay-exchange",new DelayQueues[]{DelayQueues.ORDER_REFUND_TIMEOUT}),//门店自提订单72小时退货
    //DIS_DELIVERY_DELAY("dis-delivery-delay-exchange",new DelayQueues[]{DelayQueues.UN_RECEIVE_RETURN}),
    //ORDER_SEND_TEMPLATE_DELAY("order-delay-exchange",new DelayQueues[]{DelayQueues.SEND_TEMPLATE}),
    //PAYMENT_DELAY("payment-delay-exchange",new DelayQueues[]{DelayQueues.PAYMENT_TIEMOUT}),

    SEND_HD_DELAY("post-hd-delay-exchange",new DelayQueues[]{DelayQueues.SEND_HD_DELAY_QUEUE}),//海鼎发送失败延迟重试
    SEND_DELIVERY_DELAY("post-delivery-delay-exchange",new DelayQueues[]{DelayQueues.SEND_DELIVERY_DELAY_TIMEOUT});//发送到配送延迟队列
    @Getter
    String exchangeName;
    @Getter
    DelayQueues[] queues;

    DelayExchange(String exchangeName, DelayQueues[] queues) {
        this.exchangeName = exchangeName;
        this.queues = queues;
    }

    public enum DelayQueues{
        ORDER_TIMEOUT("order-timeout","order-timeout-callback"),
        ORDER_FINISHED_TIMEOUT("order-finished-timeout","order-finished-timeout-callback"),
        ORDER_NOTICE_TIMEOUT("order-notice-timeout","order-notice-timeout-callback"),
        ORDER_REFUND_TIMEOUT("order-refund-timeout","order-refund-timeout-callback"),
        //S_ORDER_TIMEOUT("small-order-timeout","small-order-timeout-callback"),
        //UN_RECEIVE_RETURN("un-receive-return","un-receive-callback"),
        //SEND_TEMPLATE("post-template-delay","order-post-template-message"),
        //PAYMENT_TIEMOUT("order-paying-timeout","small-order-paying-callback"),
        SEND_HD_DELAY_QUEUE("post-hd-delay-queue","order-post-hd-replay"),
        SEND_DELIVERY_DELAY_TIMEOUT("post-delivery-delay-timeout","post-delivery-delay-timeout-callback");
        //FRUIT_DOCTOR_ORDER_TIMEOUT("fruit-doctor-order-timeout","fruit-doctor-order-timeout-callback");
        //WXSMALL_SHOP_ORDER_TIMEOUT("wxsmall-shop-order-timeout","wxsmall-shop-order-timeout-callback");
        @Getter
        String delayName;
        @Getter
        String forwardName;

        DelayQueues(String delayName, String forwardName) {
            this.delayName = delayName;
            this.forwardName = forwardName;
        }
    }
}