package com.lhiot.oc.delivery.meituan;

import com.lhiot.oc.delivery.config.MeiTuanProperties;
import com.lhiot.oc.delivery.meituan.model.*;
import com.lhiot.oc.delivery.meituan.util.DateUtil;
import com.lhiot.oc.delivery.meituan.util.HttpClient;
import com.lhiot.oc.delivery.meituan.util.OpenSignHelper;
import com.lhiot.oc.delivery.meituan.util.ParamBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.util.Map;

/**
 * @author yijun
 * 美团配送客户端
 */
@Slf4j
public class MeiTuanDeliveryClient {


    private MeiTuanProperties properties;
    private HttpClient httpClient;
    private OpenSignHelper openSignHelper;

    public MeiTuanDeliveryClient(MeiTuanProperties properties) {
        this.properties = properties;
        this.openSignHelper = new OpenSignHelper(properties);
        this.httpClient = new HttpClient(properties);
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
     * 发送美团业务命令
     *
     * @param request 数据
     * @param api     接口uri
     * @return JSON String
     * @throws IOException from HTTP request
     */
    private String send(@NonNull AbstractRequest request, @NonNull String api) throws IOException {

        request.setAppkey(this.properties.getAppKey());
        request.setTimestamp(DateUtil.unixTime());
        request.setVersion(this.properties.getVersion());

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
}
