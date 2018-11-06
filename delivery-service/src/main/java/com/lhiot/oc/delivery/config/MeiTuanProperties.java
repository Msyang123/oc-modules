package com.lhiot.oc.delivery.config;

import lombok.Data;

/**
 * 美团内部是通过正式账号与测试账号分开属性存储，我们的是直接加载不同的环境配置来获取
 */
@Data
public class MeiTuanProperties {

    /**
     * appkey，用于线上环境真实发单
     */
    private String appKey;

    /**
     * secret，用于线上环境真实发单
     */
    private String appSecret;

    private String url;

    private String charset;

    private String version;
}
