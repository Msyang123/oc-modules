package com.lhiot.oc.delivery.entity;

import com.lhiot.oc.delivery.model.DeliverStatus;
import lombok.Data;
import lombok.ToString;

/**
 * 配送状态流转记录实体类
 */
@Data
@ToString
public class DeliverFlow {

    private Long id;

    /**
     * 配送单id
     */
    private Long deliverNoteId;

    /**
     * 当前状态
     */
    private DeliverStatus status;

    /**
     * 上一步状态
     */
    private DeliverStatus preStatus;

    /**
     * 创建时间
     */
    private java.util.Date createAt;

}
