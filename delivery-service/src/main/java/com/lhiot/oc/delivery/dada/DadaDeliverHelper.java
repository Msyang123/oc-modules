package com.lhiot.oc.delivery.dada;

import com.leon.microx.util.GpsUtil;
import com.leon.microx.util.Maps;
import com.leon.microx.util.Pair;
import com.lhiot.oc.delivery.config.DadaProperties;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;


/**
 * @author liuyo on 17.8.5.
 */
public class DadaDeliverHelper {

    private DadaProperties properties;

    public DadaDeliverHelper(DadaProperties properties) {
        this.properties = properties;
    }

    /**
     * 签名
     *
     * @param jsonString JSON参数
     * @return 签名后的Map
     */
    Map<String, Object> sign(String jsonString) {
        SortedMap<String, Object> map = (SortedMap<String, Object>) Maps.<String, Object>as(new TreeMap<>())
                .put("format", properties.getFormat())
                .put("timestamp", Instant.now().toEpochMilli())
                .put("app_key", properties.getAppKey())
                .put("v", properties.getVersion())
                .put("source_id", properties.getSourceId())
                .put("body", jsonString).build();
        StringBuilder s = new StringBuilder();
        map.forEach((key, value) -> s.append(key).append(value));
        map.put("signature", md5(properties.getAppSecret() + s + properties.getAppSecret(), true));
        return map;
    }

    /**
     * 验签（远端签名后，本地验签）
     *
     * @param clientId   客户端ID
     * @param orderId    订单ID
     * @param updateTime 修改时间戳
     * @param signature  签名串
     * @return 验签是否通过
     */
    public boolean inspect(Object clientId, Object orderId, Object updateTime, Object signature) {
        //第一步：将参与签名的字段的值进行升序排列
        List<Object> list = new ArrayList<>();
        list.add(clientId);
        list.add(orderId);
        list.add(updateTime);
        StringBuilder signStr = new StringBuilder();
        list.stream().sorted().forEach(signStr::append);
        //第二步：将排序过后的参数，进行字符串拼接
        String sign = md5(signStr.toString(), false);
        return (signature.equals(sign));
    }

    public String post(String jsonString, String uri) throws IOException {
        URL url = new URL(properties.getUrl() + uri);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.addRequestProperty("Accept", "application/json");
        conn.addRequestProperty("Content-Type", "application/json;charset=" + properties.getCharset());
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonString.getBytes(properties.getCharset()));
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

    private static final String DIGITS = "0123456789abcdef";

    private String md5(final String str, boolean upperCase) {
        char[] digest = upperCase ? DIGITS.toUpperCase().toCharArray() : DIGITS.toLowerCase().toCharArray();
        char[] out;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(str.getBytes(this.properties.getCharset()));
            byte[] data = md5.digest();

            final int l = data.length;
            out = new char[l << 1];
            for (int i = 0, j = 0; i < l; i++) {
                out[j++] = digest[(0xF0 & data[i]) >>> 4];
                out[j++] = digest[0x0F & data[i]];
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new String(out);
    }

    /**
     * 坐标系转换
     *
     * @param lng     经度
     * @param lat     纬度
     * @param isGcj02 是否为高德坐标系（如果不是高德坐标系，则需要转换。例如百度坐标系）
     * @return {@link Pair} first=lat  second=lng
     */
    Pair<Double, Double> convertCoordinates(double lng, double lat, boolean isGcj02) {
        if (isGcj02) {
            return Pair.of(lat, lng);
        }
        double[] gcj02 = GpsUtil.bd09_To_Gcj02(lat, lng);
        return Pair.of(gcj02[0], gcj02[1]);
    }
}
