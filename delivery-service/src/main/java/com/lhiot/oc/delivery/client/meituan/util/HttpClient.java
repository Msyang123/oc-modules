package com.lhiot.oc.delivery.client.meituan.util;

import com.leon.microx.util.StringUtils;
import com.lhiot.oc.delivery.client.meituan.MeiTuanClient;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 发送Http请求工具类, get post请求
 */
@Slf4j
public class HttpClient {

    private static final int DEFAULT_TIMEOUT = 10000;
    private MeiTuanClient.Config properties;

    public HttpClient(MeiTuanClient.Config properties) {
        this.properties = properties;
    }

    /**
     * post 方法
     *
     * @param uri
     * @param params
     * @return
     * @throws IOException
     */
    public String post(String uri, Map<String, String> params) throws IOException {
        if (StringUtils.isEmpty(uri) || params == null || params.isEmpty()) {
            return "";
        }
        HttpPost httpPost = new HttpPost(this.properties.getUrl() + uri);
        RequestConfig requestConfig = RequestConfig
                .custom()
                .setSocketTimeout(DEFAULT_TIMEOUT)
                .setConnectTimeout(DEFAULT_TIMEOUT)
                .build();//设置请求和传输超时时间

        httpPost.setConfig(requestConfig);
        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        List<BasicNameValuePair> basicNameValuePairs = new ArrayList<>();
        for (Map.Entry<String, String> entity : params.entrySet()) {
            basicNameValuePairs.add(new BasicNameValuePair(entity.getKey(), entity.getValue()));
        }

        UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(basicNameValuePairs, this.properties.getCharset());
        httpPost.setEntity(urlEncodedFormEntity);

        @Cleanup CloseableHttpClient httpClient = HttpClients.createDefault();
        @Cleanup CloseableHttpResponse response = httpClient.execute(httpPost);

        StatusLine statusLine = response.getStatusLine();
        log.info("request url: {}, params: {}, response status: {}", uri, params.toString(), statusLine.getStatusCode());
        String result = EntityUtils.toString(response.getEntity(), this.properties.getCharset());
        log.info("response data: {}", result);
        return result == null ? "" : result.trim();
    }

}
