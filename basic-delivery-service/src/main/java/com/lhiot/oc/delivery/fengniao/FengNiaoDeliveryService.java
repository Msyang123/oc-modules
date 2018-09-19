package com.lhiot.oc.delivery.fengniao;

import com.leon.microx.util.Jackson;
import com.leon.microx.util.Maps;
import com.leon.microx.util.auditing.Random;
import com.lhiot.oc.delivery.config.FengNiaoProperties;
import com.lhiot.oc.delivery.fengniao.util.HttpClient;
import com.lhiot.oc.delivery.fengniao.util.OpenSignHelper;
import com.lhiot.oc.delivery.fengniao.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Leon (234239150@qq.com) created in 8:45 18.9.19
 */
@Slf4j
public class FengNiaoDeliveryService {
    private FengNiaoProperties properties;
    private HttpClient httpClient;
    private OpenSignHelper openSignHelper;

    public FengNiaoDeliveryService(FengNiaoProperties properties) {
        this.properties = properties;
        this.openSignHelper = new OpenSignHelper(properties);
        this.httpClient = new HttpClient(properties);
    }

    /**
     * 下单
     *
     * @param request 请求参数
     * @param token   access token
     * @return JSON String
     */
    public String deliver(ElemeCreateOrderRequest.ElemeCreateRequestData request, TokenResponse token) throws IOException {
        request.setNotifyUrl(this.properties.getBackUrl());
        return this.send(new ElemeCreateOrderRequest(), request, FengNiaoServerApi.ORDER_CREATE, token);
    }

    /**
     * 查询蜂鸟配送订单
     *
     * @param request 请求参数
     * @param token   access token
     * @return JSON String
     */
    public String one(AbstractRequestData request, TokenResponse token) throws IOException {
        return this.send(new ElemeQueryOrderRequest(), request, FengNiaoServerApi.ORDER_QUERY, token);
    }

    /**
     * 取消订单
     *
     * @param request 请求参数
     * @param token   access token
     * @return JSON String
     */
    public String cancel(ElemeCancelOrderRequest.ElemeCancelOrderRequstData request, TokenResponse token) throws IOException {
        request.setOrderCancelNotifyUrl(this.properties.getBackUrl());
        return this.send(new ElemeCancelOrderRequest(), request, FengNiaoServerApi.ORDER_CANCEL, token);
    }

    /**
     * 投诉订单
     *
     * @param request 请求参数
     * @param token   access token
     * @return JSON String
     */
    public String complain(ElemeOrderComplaintRequest.ElemeOrderComplaintRequstData request, TokenResponse token) throws IOException {
        return this.send(new ElemeOrderComplaintRequest(), request, FengNiaoServerApi.ORDER_COMPLAINT, token);
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
        request.setAppId(this.properties.getAppKey());
        request.setSalt(salt);
        request.setData(data);
        request.setSignature(openSignHelper.generateBusinessSign(
                Maps.of("app_id", this.properties.getAppKey(), "access_token", accessToken, "data", request.getData(), "salt", salt)
        ));
        String requestJson = Jackson.json(request);
        return httpClient.post(api, requestJson);
    }

    /**
     * 获取蜂鸟配送Token
     * 需要存储到服务器全局变量 1小时刷新一次
     * 不允许直接在调用接口使用，应该从缓存中获取
     *
     * @return TokenResponse
     */
    @Nullable
    public TokenResponse accessToken() {
        int salt = Random.ofInt(4);
        Map<String, Object> map = new HashMap<>(4);
        map.put("app_id", this.properties.getAppKey());
        map.put("salt", salt);

        String signature = openSignHelper.generateSign(this.properties.getAppKey(), salt + "", this.properties.getAppSecret());
        try {
            map.put("signature", signature);
            log.info("输出salt值:" + map.get("salt"));
            // 建立远程连接蜂鸟,获取access_token
            String getTokenResult = httpClient.get(FengNiaoServerApi.OBTAIN_TOKEN, map);
            log.info("token" + getTokenResult);
            Map resultMap = Jackson.map(getTokenResult);
            boolean success = Objects.nonNull(resultMap) && Objects.equals("200", resultMap.get("code"));
            if (!success) {
                log.error("蜂鸟签名返回结果不正确" + getTokenResult);
                return null;
            }
            return Jackson.object(getTokenResult, TokenResponse.class);
        } catch (UnsupportedEncodingException e) {
            log.error("蜂鸟签名Ecode转换错误" + e.getMessage());
        } catch (IOException e) {
            log.error("蜂鸟签名通讯错误" + e.getMessage());
        }
        return null;
    }
}
