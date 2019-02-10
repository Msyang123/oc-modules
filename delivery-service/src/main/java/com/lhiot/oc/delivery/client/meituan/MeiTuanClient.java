package com.lhiot.oc.delivery.client.meituan;

import com.lhiot.oc.delivery.client.meituan.model.*;
import com.lhiot.oc.delivery.client.meituan.util.DateUtil;
import com.lhiot.oc.delivery.client.meituan.util.HttpClient;
import com.lhiot.oc.delivery.client.meituan.util.OpenSignHelper;
import com.lhiot.oc.delivery.client.meituan.util.ParamBuilder;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.util.Map;

/**
 * @author yijun
 * 美团配送客户端
 */
@Slf4j
public class MeiTuanClient {

    private Config config;
    private HttpClient httpClient;
    private OpenSignHelper openSignHelper;

    public MeiTuanClient(Config config) {
        this.config = config;
        this.openSignHelper = new OpenSignHelper(config);
        this.httpClient = new HttpClient(config);
    }

    /**
     * 下单
     *
     * @param request 请求参数
     * @return JSON String
     */
    public String deliver(CreateOrderByShopRequest request) throws IOException {
        return this.send(request, MeiTuanApi.ORDER_CREATE_BY_SHOP);
    }

    /**
     * 查询美团配送订单
     *
     * @param request 请求参数
     * @return JSON String
     */
    public String one(QueryOrderRequest request) throws IOException {
        return this.send(request, MeiTuanApi.ORDER_QUERY);
    }

    /**
     * 取消订单
     *
     * @param request 请求参数
     * @return JSON String
     */
    public String cancel(CancelOrderRequest request) throws IOException {
        return this.send(request, MeiTuanApi.ORDER_CANCEL);
    }

    /**
     * 回调签名验证
     * @param params
     * @return
     */
    public String backSignature(Map<String, String> params){
        return this.openSignHelper.generateSign(params);
    }

    /**
     * 发送美团业务命令
     *
     * @param request 数据
     * @param api     接口uri
     * @return JSON String
     * @throws IOException from HTTP request
     */
    private String send(@NonNull AbstractRequest request, @NonNull String api) throws IOException {

        request.setAppkey(this.config.getAppKey());
        request.setTimestamp(DateUtil.unixTime());
        request.setVersion(this.config.getVersion());

        //将request转换为Map
        Map<String, String> params = ParamBuilder.convertToMap(request);
        //使用签名
        String sign = this.openSignHelper.generateSign(params);
        params.put("sign", sign);

        return httpClient.post(api, params);
    }

    /**********************模拟配送端*******************************************************/
    /**
     * 模拟配送端发起操作
     * 接收
     *
     * @param request
     * @return
     */

    public String accept(MockOrderRequest request) throws IOException {

        return send(request, MeiTuanApi.MOCK_ORDER_ACCEPT);
    }

    /**
     * 模拟配送端发起操作
     * 取货
     *
     * @param request
     * @return
     */
    public String fetch(MockOrderRequest request) throws IOException {

        return send(request, MeiTuanApi.MOCK_ORDER_PICKUP);
    }

    /**
     * 模拟配送端发起操作
     * 完成配送
     *
     * @param request
     * @return
     */
    public String finish(MockOrderRequest request) throws IOException {

        return send(request, MeiTuanApi.MOCK_ORDER_DELIVER);
    }

    /**
     * 模拟配送端发起操作
     * 改派骑手
     *
     * @param request
     * @return
     */
    public String rearrange(MockOrderRequest request) throws IOException {

        return send(request, MeiTuanApi.MOCK_ORDER_REARRANGE);
    }

    /**********************模拟配送端*******************************************************/

    @Data
    @ToString
    public static class Config {

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
}
