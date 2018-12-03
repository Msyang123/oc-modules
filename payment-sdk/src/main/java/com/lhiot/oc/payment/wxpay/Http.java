package com.lhiot.oc.payment.wxpay;

import com.leon.microx.util.xml.XReader;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.springframework.lang.Nullable;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author Leon (234239150@qq.com) created in 10:02 18.12.1
 */
@Slf4j
public class Http {

    private static final String BASE_DOMAIN = "https://api.mch.weixin.qq.com";

    private static final String SANDBOX_BASE_DOMAIN = "https://api.mch.weixin.qq.com/sandboxnew";

    private static final String CHARSET_UTF_8 = "UTF-8";

    private static final RequestConfig DEFAULT_REQUEST_CONFIG = RequestConfig.DEFAULT;

    private final boolean sandbox;

    private Http(boolean sandbox) {
        this.sandbox = sandbox;
    }

    public static Http client() {
        return new Http(false);
    }

    public static Http sandbox() {
        return new Http(true);
    }

    public XReader post(Api api, Map<String, Object> data) {
        try {
            HttpPost httpPost = new HttpPost((sandbox ? SANDBOX_BASE_DOMAIN : BASE_DOMAIN) + api.getUri());
            httpPost.setEntity(new StringEntity(toXml(data), CHARSET_UTF_8));
            @Cleanup CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(DEFAULT_REQUEST_CONFIG).build();
            @Cleanup CloseableHttpResponse response = httpClient.execute(httpPost);
            String xml = EntityUtils.toString(response.getEntity(), CHARSET_UTF_8);
            return XReader.of(xml);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return XReader.empty();
    }

    public XReader post(Api api, Map<String, Object> data, Supplier<SSLConnectionSocketFactory> supplier) {
        SSLConnectionSocketFactory sslConnection = supplier.get();
        if (Objects.nonNull(sslConnection)) {
            try {
                HttpPost httpPost = new HttpPost((sandbox ? SANDBOX_BASE_DOMAIN : BASE_DOMAIN) + api.getUri());
                httpPost.setEntity(new StringEntity(toXml(data), CHARSET_UTF_8));
                @Cleanup CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(DEFAULT_REQUEST_CONFIG).setSSLSocketFactory(sslConnection).build();
                @Cleanup CloseableHttpResponse response = httpClient.execute(httpPost);
                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                    throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
                }
                String resource = EntityUtils.toString(response.getEntity(), CHARSET_UTF_8);
                String xml = resource.replace("<![CDATA[", "").replace("]]>", "");
                return XReader.of(xml);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return XReader.empty();
    }

    @Nullable
    public static Supplier<SSLConnectionSocketFactory> ssl(InputStream cert, char[] password) {
        try {
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            keystore.load(cert, password);
            SSLContext sslContext = SSLContexts.custom().loadKeyMaterial(keystore, password).build();
            return () -> new SSLConnectionSocketFactory(
                    sslContext,
                    new String[]{"TLSv1", "TLSv1.1", "TLSv1.2"}, null,
                    SSLConnectionSocketFactory.getDefaultHostnameVerifier()
            );
        } catch (IOException | GeneralSecurityException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 【微信支付】 将请求参数转换为xml格式的string
     */
    private String toXml(final Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("<xml>");
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String k = entry.getKey();
            Object v = entry.getValue();
            if ("sign".equalsIgnoreCase(k)) {
                continue;
            }
            if ("attach".equalsIgnoreCase(k) || "body".equalsIgnoreCase(k)) {
                sb.append("<").append(k).append("><![CDATA[").append(v).append("]]></").append(k).append(">");
            } else {
                sb.append("<").append(k).append(">").append(v).append("</").append(k).append(">");
            }
        }
        sb.append("<sign>").append(data.get("sign")).append("</sign>");
        return sb.append("</xml>").toString();
    }
}
