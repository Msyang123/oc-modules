package com.lhiot.oc.delivery.client.meituan.util;

import com.leon.microx.util.StringUtils;
import com.lhiot.oc.delivery.client.meituan.MeiTuanClient;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 美团签名计算工具类
 */
@Slf4j
public class OpenSignHelper {

    private MeiTuanClient.Config properties;

    public OpenSignHelper(MeiTuanClient.Config properties) {
        this.properties = properties;
    }

    public String generateSign(Map<String, String> params) {
        String encodeString = getEncodeString(params);
        log.info(String.format("encodeString: %s", encodeString));
        String sign = SHA1Util.Sha1(encodeString);
        log.info(String.format("generateSign: %s", sign));
        return sign;
    }

    private String getEncodeString(Map<String, String> params) {
        Iterator<String> keyIter = params.keySet().iterator();
        Set<String> sortedParams = new TreeSet<>();
        while (keyIter.hasNext()) {
            sortedParams.add(keyIter.next());
        }

        StringBuilder strB = new StringBuilder(this.properties.getAppSecret());

        // 排除sign和空值参数
        for (String key : sortedParams) {
            if (key.equals("sign")) {
                continue;
            }
            String value = params.get(key);

            if (StringUtils.isNotEmpty(value)) {
                strB.append(key).append(value);
            }
        }
        return strB.toString();
    }
}
