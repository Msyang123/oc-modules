package com.lhiot.oc.basic.service;

import com.leon.microx.common.wrapper.Tips;
import com.lhiot.order.domain.DeliverFee;
import com.lhiot.order.domain.inparam.CreateOrderParam;

/**
 * 配送平台统一流程接口
 * @author yj
 */
public interface IDelivery {

    /**
     * 发送订单到配送平台
     * @param hdOrderCode
     * @return
     */
    String send(String hdOrderCode);

    /**
     * 配送平台回调处理
     * @param backMsg
     * @return
     */
    Tips callBack(String backMsg);

    /**
     * 取消配送平台配送单
     * @param hdOrderCode
     * @param cancelReasonId
     * @param cancelReason
     * @return
     */
    Tips cancel(String hdOrderCode, int cancelReasonId, String cancelReason);

    /**
     * 查询配送平台订单信息
     * @param hdOrderCode
     * @return
     */
    String detail(String hdOrderCode);

    /**
     * 查询配送费
     * @param deliveryAddress
     * @param orderParam
     * @return
     */
    Tips<DeliverFee> queryDeliverFee(String deliveryAddress, CreateOrderParam orderParam);

}
