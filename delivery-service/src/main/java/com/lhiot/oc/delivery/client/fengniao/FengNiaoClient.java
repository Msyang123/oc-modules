package com.lhiot.oc.delivery.client.fengniao;

import com.leon.microx.util.Jackson;
import com.leon.microx.util.Maps;
import com.leon.microx.util.auditing.MD5;
import com.leon.microx.util.auditing.Random;
import com.lhiot.oc.delivery.client.fengniao.model.*;
import com.lhiot.oc.delivery.client.fengniao.util.HttpClient;
import com.lhiot.oc.delivery.client.fengniao.util.OpenSignHelper;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author Leon (234239150@qq.com) created in 8:45 18.9.19
 */
@Slf4j
public class FengNiaoClient {
    private Config config;
    private HttpClient httpClient;
    private OpenSignHelper openSignHelper;
    private final RedissonClient redissonClient;


    public FengNiaoClient(Config config, RedissonClient redissonClient) {
        this.config = config;
        this.openSignHelper = new OpenSignHelper(config);
        this.httpClient = new HttpClient(config);
        this.redissonClient = redissonClient;
    }

    /**
     * 下单
     *
     * @param request 请求参数
     * @param token   access token
     * @return JSON String
     */
    public String deliver(ElemeCreateOrderRequest.ElemeCreateRequestData request, TokenResponse token) throws IOException {
        return this.send(new ElemeCreateOrderRequest(), request, FengNiaoApi.ORDER_CREATE, token);
    }

    /**
     * 查询蜂鸟配送订单
     *
     * @param request 请求参数
     * @param token   access token
     * @return JSON String
     */
    public String one(AbstractRequestData request, TokenResponse token) throws IOException {
        return this.send(new ElemeQueryOrderRequest(), request, FengNiaoApi.ORDER_QUERY, token);
    }

    /**
     * 取消订单
     *
     * @param request 请求参数
     * @param token   access token
     * @return JSON String
     */
    public String cancel(ElemeCancelOrderRequest.ElemeCancelOrderRequstData request, TokenResponse token) throws IOException {
        request.setOrderCancelNotifyUrl(this.config.getBackUrl());
        return this.send(new ElemeCancelOrderRequest(), request, FengNiaoApi.ORDER_CANCEL, token);
    }

    /**
     * 投诉订单
     *
     * @param request 请求参数
     * @param token   access token
     * @return JSON String
     */
    public String complain(ElemeOrderComplaintRequest.ElemeOrderComplaintRequstData request, TokenResponse token) throws IOException {
        return this.send(new ElemeOrderComplaintRequest(), request, FengNiaoApi.ORDER_COMPLAINT, token);
    }

    /**
     * 发送蜂鸟业务命令
     *
     * @param data          数据
     * @param api           接口uri
     * @param tokenResponse 蜂鸟access token
     * @return JSON String
     * @throws IOException from HTTP request
     */
    private String send(@NonNull AbstractRequest request, AbstractRequestData data, @NonNull String api, @NonNull TokenResponse tokenResponse) throws IOException {
        String accessToken = tokenResponse.getData().getAccessToken();
        int salt = Random.ofInt(4);
        request.setAppId(this.config.getAppKey());
        request.setSalt(salt);
        request.setData(data);
        request.setSignature(openSignHelper.generateBusinessSign(
                Maps.of("app_id", this.config.getAppKey(), "access_token", accessToken, "data", request.getData(), "salt", salt)
        ));
        String requestJson = Jackson.json(request);
        return httpClient.post(api, requestJson);
    }

    public String backSignature(String appId, String data, String salt, @NonNull TokenResponse tokenResponse) {
        StringBuffer needSignatureStr = new StringBuffer();

        String accessToken = tokenResponse.getData().getAccessToken();
        needSignatureStr.append("app_id=").append(appId).append("&").append("access_token=").append(accessToken).append("&data=").append(data).append("&").append("salt=").append(salt);
        return MD5.str(needSignatureStr.toString());
    }

    /**
     * 获取蜂鸟配送Token
     * 需要存储到服务器全局变量 1小时刷新一次
     * 不允许直接在调用接口使用，应该从缓存中获取
     *
     * @return TokenResponse
     */
    @Nullable
    public TokenResponse accessToken(String cacheName) {
        TokenResponse token = null;
        RBucket<TokenResponse> accessToken = null;
        if (Objects.nonNull(redissonClient)) {
            accessToken = redissonClient.getBucket(cacheName);
            token = accessToken.get();
        }
        try {
            if (Objects.isNull(token)) {
                int salt = Random.ofInt(4);
                String responseTokenJson = httpClient.get(FengNiaoApi.OBTAIN_TOKEN, Maps.of(
                        "app_id", this.config.getAppKey(),
                        "salt", salt,
                        "signature", openSignHelper.generateSign(this.config.getAppKey(), salt + "", this.config.getAppSecret())
                ));
                token = Jackson.object(responseTokenJson, TokenResponse.class);
                if (Objects.equals("200", token.getCode()) && Objects.nonNull(accessToken)) {
                    accessToken.setAsync(token, 1, TimeUnit.HOURS);
                }
            }
        } catch (IOException e) {
            log.error("从蜂鸟服务端获取AccessToken失败！- " + e.getMessage(), e);
        }
        return token;
    }

    @Data
    @ToString
    public static class Config {

//        /**
//         * 饿了么开放平台api<br>
//         * 联调环境地址 https://exam-anubis.ele.me/anubis-webapi <br>
//         * 线上环境地址 https://open-anubis.ele.me/anubis-webapi
//         */
//        public static final String API_URL = "https://exam-anubis.ele.me/anubis-webapi";
//
//        /**
//         * 第三方平台 app_id
//         */
//        private String appId = "e6641c74-5fbd-424f-a93e-c25327f15d2b";
//        /**
//         * 第三方平台 secret_key
//         */
//        private String secretKey = "68ddfdc0-6bbd-4d61-9122-eb7aaa6eaf75";

        private String appKey;
        private String appSecret;
        private String charset = "UTF-8";
        private String version;
        private String url;
        private String backUrl;//回调第三方服务地址
    }
}
