package com.lhiot.oc.basic.domain.enums;

import lombok.Getter;

public enum  PublishExchange {
    /**例子
     * 每新增一个信道便新增一个枚举类型，name 表示信道名称 ，queues 表示绑定在改信道上的队列数组
     */
     CREATE_EXCHANGE("order-create-event",new PublishQueue[]{PublishQueue.ORDER_CREATE_PUBLISHER}),
     PAYMENT_EXCHANGE("order-paid-event",new PublishQueue[]{PublishQueue.PAID_TEAMBUY}),
     SIGN_EXCHANGE("order-sign-event",new PublishQueue[]{PublishQueue.SIGN_TEAMBUY}),
     REFUND_EXCHANGE("order-refund-event",new PublishQueue[]{PublishQueue.REFUND_TEAMBUY}),
     TEAMBUY_DELAY_EXCHANGE("order-paid-delay-event",new PublishQueue[]{PublishQueue.DELAY_TEAMBUY}),
	 //FRUITDOCOTR_PAYMENT("fruitdoctor-order-paid-event",new PublishQueue[]{PublishQueue.FRUIT_DOCTOR_MALL_BONUS});
     PUBLISH_PAYMENT_NOTIFIED("publish-payment-notified",new PublishQueue[]{PublishQueue.PUBLISH_PAYMENT});

 @Getter
     String name;
     @Getter
     PublishQueue[] queues;

     PublishExchange(String name, PublishQueue[] queues) {
     this.name = name;
     this.queues = queues;
     }

     public enum PublishQueue{
     ORDER_CREATE_PUBLISHER("order-create-publisher"),
     PAID_TEAMBUY("order-paid-teambuy-publisher"),
     SIGN_TEAMBUY("order-sign-teambuy-publisher"),
     REFUND_TEAMBUY("order-refund-teambuy-publisher"),
     DELAY_TEAMBUY("order-paid-delay-publisher"),
     //FRUIT_DOCTOR_MALL_BONUS("calculation_bonus");
     PUBLISH_PAYMENT("publish-payment-publisher");
     @Getter
     String name;

     PublishQueue(String name) {
     this.name = name;
     }
     }

}
