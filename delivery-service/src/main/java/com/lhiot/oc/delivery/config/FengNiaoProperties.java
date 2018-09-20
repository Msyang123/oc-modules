package com.lhiot.oc.delivery.config;

import lombok.Data;

@Data
public class FengNiaoProperties {

	/**
	 * 饿了么开放平台api<br>
	 * 联调环境地址 https://exam-anubis.ele.me/anubis-webapi <br>
	 * 线上环境地址 https://open-anubis.ele.me/anubis-webapi
	 */
	/*public static final String API_URL = "https://exam-anubis.ele.me/anubis-webapi";

	*//**
	 * 第三方平台 app_id
	 *//*
	private String appId = "e6641c74-5fbd-424f-a93e-c25327f15d2b";

	*//**
	 * 第三方平台 secret_key
	 *//*
	private String secretKey = "68ddfdc0-6bbd-4d61-9122-eb7aaa6eaf75";*/
	
    private String appKey;
    private String appSecret;
    private String charset = "UTF-8";
    private String version;
    private String url;
	private String backUrl;//回调第三方服务地址
}
