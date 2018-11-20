package com.lhiot.oc.delivery.client.meituan.model;

import lombok.Data;

/**
 * 抽象响应父类
 */
@Data
public abstract class AbstractResponse {
    protected String code;
    protected String message;
}
