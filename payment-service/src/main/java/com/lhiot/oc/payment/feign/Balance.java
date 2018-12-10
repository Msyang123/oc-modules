package com.lhiot.oc.payment.feign;

import lombok.Data;
import lombok.ToString;

/**
 * @author Leon (234239150@qq.com) created in 13:28 18.11.28
 */
@Data
@ToString
public class Balance {

    private String applicationType;

    private Long money;

    private Operation operation;

    private String sourceType;

    private String sourceId;

    private String password;

    public enum Operation {
        SUBTRACT, ADD
    }
}
