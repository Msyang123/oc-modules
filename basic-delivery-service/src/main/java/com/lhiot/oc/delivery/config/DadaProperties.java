package com.lhiot.oc.delivery.config;

import lombok.Data;

/**
 * @author liuyo on 17.8.5.
 * @author maoxianzhi on 17.10.22
 */


@Data
public class DadaProperties {

    private String appKey;
    private String appSecret;
    private String url;

    private String sourceId;
    private String version;
    private String format = "json";
    private String charset = "UTF-8";
    private String backUrl;//回调第三方服务地址
}
