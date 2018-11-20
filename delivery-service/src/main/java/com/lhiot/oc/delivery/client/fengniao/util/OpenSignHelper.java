package com.lhiot.oc.delivery.client.fengniao.util;

import com.leon.microx.util.StringUtils;
import com.leon.microx.util.auditing.MD5;
import com.lhiot.oc.delivery.client.fengniao.FengNiaoClient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Set;

/**
 * 签名 获取token
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenSignHelper {
    private static final Log logger = LogFactory.getLog(OpenSignHelper.class);

    private FengNiaoClient.Config properties;

    /**
     * 请求token时生成签名
     *
     * @return
     */
    public String generateSign(String appId, String salt, String secretKey) {
        StringBuilder seed = new StringBuilder();
        seed.append("app_id=").append(appId).append("&salt=").append(salt).append("&secret_key=").append(secretKey);
        String sign = "";
        try {
            String encodeString = URLEncoder.encode(seed.toString(), this.properties.getCharset());
            sign = MD5.asHex(encodeString);
            logger.info(String.format("querySting: %s, encodeString: %s, sign: %s", seed.toString(), encodeString, sign));
        } catch (UnsupportedEncodingException e) {
            logger.error("不支持的编码类型, %s", e);
        }
        return sign;
    }

    /**
     * 业务请求生成签名
     */
    public String generateBusinessSign(Map<String, Object> sigMap) {
        StringBuilder seed = new StringBuilder();
        Set<String> set = sigMap.keySet();
        for (String key : set) {
            seed.append(key).append("=").append(sigMap.get(key)).append("&");
        }
        String queryString = seed.substring(0, seed.length() - 1);
        logger.info(String.format("query string is %s", queryString));
        //MD5Utils.getMD5Code()
        return MD5.asHex(queryString);
    }

    // 计算蜂鸟回调数据的签名
    public boolean backSignature(String appId, String data, String salt, String accessToken, String signature) {
        if (StringUtils.isBlank(signature))
            return false;
        // 判定签名是否正确 防止被篡改
        StringBuilder needSignatureStr = new StringBuilder();

        logger.info(accessToken);
        // 签名规则
        needSignatureStr
                .append("app_id=")
                .append(appId)
                .append("&")
                .append("access_token=")
                .append(accessToken)
                .append("&data=")
                .append(data)
                .append("&")
                .append("salt=")
                .append(salt);
        return signature.equals(MD5.asHex(needSignatureStr.toString()));
    }
}
