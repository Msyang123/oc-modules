package com.lhiot.oc.delivery.fengniao.util;

import com.lhiot.oc.delivery.config.FengNiaoProperties;
import lombok.Data;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * 发送Http请求工具类, get post请求
 */
@Data
public class HttpClient {

    private FengNiaoProperties properties;
    private static final Log logger = LogFactory.getLog(HttpClient.class);

    public HttpClient(FengNiaoProperties properties) {
        this.properties = properties;
    }

    public String get(String uri, Map<String, Object> params) throws IOException {
        StringBuilder urlStr = new StringBuilder(properties.getUrl());
        urlStr.append(uri).append("?");
        for (Map.Entry<String, Object> item : params.entrySet()) {
            urlStr.append(item.getKey()).append("=").append(item.getValue().toString()).append("&");
        }
        String queryString = urlStr.substring(0, urlStr.length() - 1);
        URL url = new URL(queryString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.addRequestProperty("Accept", "application/json");
        conn.addRequestProperty("Content-Type", "application/json;charset=" + this.properties.getCharset());
        conn.setRequestMethod("GET");

        StringBuilder result = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                result.append(line);
            }
        }
        logger.debug(result);
        return result.toString();
    }

    public String post(String uri, String body) throws IOException {
        URL url = new URL(this.properties.getUrl() + "/" + this.properties.getVersion() + uri);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.addRequestProperty("Accept", "application/json");
        conn.addRequestProperty("Content-Type", "application/json;charset=" + this.properties.getCharset());
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(this.properties.getCharset()));
            os.flush();
        }

        StringBuilder result = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                result.append(line);
            }
        }
        return result.toString();
    }

}
