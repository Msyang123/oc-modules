package com.lhiot.oc.delivery.client.fengniao;

/**
 * @author Leon (234239150@qq.com) created in 15:52 18.9.18
 */
public interface FengNiaoApi {
    /**
     * 获取token
     */
    String OBTAIN_TOKEN = "/get_access_token";

    /**
     * 创建订单
     */
    String ORDER_CREATE = "/order";

    /**
     * 取消 订单
     */
    String ORDER_CANCEL = "/order/cancel";

    /**
     * 订单查询
     */
    String ORDER_QUERY = "/order/query";

    /**
     * 订单投诉
     */
    String ORDER_COMPLAINT = "/order/complaint";

    /**
     * 查询配送费
     */
    String DELIVERY_QUERY = "/chain_store/delivery/query";
}
