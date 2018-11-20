package com.lhiot.oc.delivery.entity;

import com.lhiot.oc.delivery.model.DeliverStatus;
import com.lhiot.oc.delivery.model.DeliverType;
import lombok.Data;
import lombok.ToString;

import java.util.Date;

/**
 * 配送单信息实体类
 */
@Data
@ToString
public class DeliverNote {

    private Long id;

    /**
     * 配送单编码
     */
    private String deliverCode;

    /**
     * 订单id
     */
    private Long orderId;

    /**
     * 创建时间
     */
    private Date createTime;


    /**
     * 配送员姓名
     */
    private String deliverName;

    /**
     * 配送员手机号
     */
    private String deliverPhone;

    /**
     * 配送距离
     */
    private Double distance;

    /**
     * 配送费
     */
    private Integer fee;

    /**
     * 订单编码
     */
    private String orderCode;

    /**
     * 配送方式 FENGNIAO-蜂鸟配送DADA-达达配送 OWN-自己配送
     */
    private DeliverType deliverType;

    /**
     * 配送失败原因
     */
    private String failureCause;

    /**
     * 配送取消时间
     */
    private Date cancelTime;


    /**
     * 接单时间
     */
    private Date receiveTime;

    /**
     * 配送状态 UNRECEIVED-未接单 WAIT_GET-待取货 DELIVERING-配送中 DONE-配送完成 FAILURE-配送失败
     */
    private DeliverStatus deliverStatus;

    /**
     * 门店编码
     */
    private String storeCode;

    /**
     * 备注
     */
    private String remark;

    /**
     * 扩展数据
     */
    private String ext;

}
