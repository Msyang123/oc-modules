package com.lhiot.oc.basic.service;

import com.leon.microx.support.result.Tips;
import com.lhiot.oc.basic.domain.DeliverBaseOrder;
import com.lhiot.oc.basic.domain.enums.DeliverNeedConver;

/**
 * 配送平台统一流程接口
 * @author yj
 */
public interface IDelivery {

    /**
     * 发送订单到配送平台
     * @param deliverNeedConver 是否需要转换坐标系 需要转成高德系标准 百度坐标系需要，腾讯坐标系不需要
     * @param deliverBaseOrder 配送的订单信息
     * @return
     */
    Tips send(DeliverNeedConver deliverNeedConver,DeliverBaseOrder deliverBaseOrder);

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
     * 取消配送订单原因列表
     * @return
     */
    String cancelOrderReasons();

    /**
     * 查询配送平台订单信息
     * @param hdOrderCode
     * @return
     */
    String detail(String hdOrderCode);

}
