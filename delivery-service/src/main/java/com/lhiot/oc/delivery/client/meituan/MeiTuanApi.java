package com.lhiot.oc.delivery.client.meituan;

//美团接口uri
public interface MeiTuanApi {
    /**
     * 开放平台接口地址，正式账号/测试账号采用同一地址
     */

    /**
     * 订单创建（门店方式）
     */
    String ORDER_CREATE_BY_SHOP = "/order/createByShop";

    /**
     * 取消订单
     */
    String ORDER_CANCEL = "/order/delete";

    /**
     * 查询订单状态
     */
    String ORDER_QUERY = "/order/status/query";

    /**
     * 模拟测试订单接单
     */
    String MOCK_ORDER_ACCEPT = "/test/order/arrange";

    /**
     * 模拟测试订单取货
     */
    String MOCK_ORDER_PICKUP = "/test/order/pickup";

    /**
     * 模拟测试订单完成
     */
    String MOCK_ORDER_DELIVER = "/test/order/deliver";

    /**
     * 模拟测试订单改派
     */
    String MOCK_ORDER_REARRANGE = "/test/order/rearrange";

    /**
     * 配送能力预校验
     */
    String ORDER_CHECK_DELIVERY_ABILITY = "/order/check";

    /**
     * 订单创建（送货分拣方式）
     */
    String ORDER_CREATE_BY_COORDINATES = "/order/createByCoordinates";

    /**
     * 评价骑手
     */
    String ORDER_EVALUATE = "/order/evaluate";
}
