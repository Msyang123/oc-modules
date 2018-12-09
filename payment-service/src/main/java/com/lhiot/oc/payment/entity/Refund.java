package com.lhiot.oc.payment.entity;

import com.lhiot.oc.payment.type.RefundStep;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

@Data
@ToString
public class Refund implements Serializable {

    private Long id;

    private Long recordId;

    private String reason;

    private Long fee;

    private RefundStep refundStep;

    private Date createAt;

    private Date completedAt;
}
