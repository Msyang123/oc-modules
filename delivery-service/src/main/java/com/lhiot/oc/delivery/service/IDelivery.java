package com.lhiot.oc.delivery.service;

import com.leon.microx.web.result.Tips;
import com.lhiot.oc.delivery.domain.DeliverBaseOrder;
import com.lhiot.oc.delivery.domain.enums.CoordinateSystem;

import java.math.BigDecimal;

/**
 * 配送平台统一流程接口
 *
 * @author yj
 */
public interface IDelivery {

    /**
     * 发送订单到配送平台
     *
     * @param coordinateSystem 坐标系
     * @param deliverBaseOrder 配送的订单信息
     * @param distance 配送距离
     * @return
     */
    Tips send(CoordinateSystem coordinateSystem, DeliverBaseOrder deliverBaseOrder, BigDecimal distance);

    /**
     * 配送平台回调处理
     *
     * @param backMsg
     * @return
     */
    Tips callback(String backMsg);

    /**
     * 取消配送平台配送单
     *
     * @param hdOrderCode
     * @param cancelReasonId
     * @param cancelReason
     * @return
     */
    Tips cancel(String hdOrderCode, int cancelReasonId, String cancelReason);

    /**
     * 取消配送订单原因列表
     *
     * @return
     */
    String cancelOrderReasons();

    /**
     * 查询配送平台订单信息
     *
     * @param hdOrderCode
     * @return
     */
    String detail(String hdOrderCode);

}
