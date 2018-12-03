package com.lhiot.oc.payment.wxpay;

import com.leon.microx.util.DateTime;
import com.leon.microx.util.Maps;
import com.leon.microx.util.StringUtils;
import com.leon.microx.util.auditing.MD5;
import com.leon.microx.util.auditing.Random;
import com.leon.microx.util.xml.XNode;
import com.leon.microx.util.xml.XReader;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * @author Leon (234239150@qq.com) created in 14:10 18.12.3
 */
@Slf4j
public class Signer {

    private Config config;
    private String clientId;
    private Type type;
    private String outTradeNo;
    private String totalFee;

    private Signer(Config config, String clientId, Type type, String outTradeNo, String totalFee) {
        this.config = config;
        this.clientId = clientId;
        this.type = type;
        this.outTradeNo = outTradeNo;
        this.totalFee = totalFee;
    }

    public static Signer app(Config config, String clientIp, String outTradeNo, String fee) {
        return new Signer(config, clientIp, Type.APP, outTradeNo, fee);
    }

    public static Signer jsApi(Config config, String clientIp, String outTradeNo, String fee) {
        return new Signer(config, clientIp, Type.JS_API, outTradeNo, fee);
    }

    public PrePay prepay() {
        return new PrePay(this);
    }

    public boolean verify(String nonceStr, String signed) {
        SortedMap<String, Object> data = new TreeMap<>();
        data.put("appid", config.getAppId());
        data.put("mch_id", config.getPartnerId());
        data.put("notify_url", config.getPayedNotifyUrl());
        data.put("spbill_create_ip", this.clientId);
        data.put("trade_type", this.type.getId());
        data.put("out_trade_no", this.outTradeNo);
        data.put("total_fee", this.totalFee);
        data.put("nonce_str", nonceStr);
        String resign = sign(config.getPartnerKey(), data);
        return resign.equals(signed);
    }

    public static String sign(String partnerKey, Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        data.forEach((k, v) -> {
            if (null != v && !"".equals(v) && !"sign".equals(k) && !"key".equals(k)) {
                sb.append(k).append("=").append(v).append("&");
            }
        });
        sb.append("key=").append(partnerKey);
        String sign = MD5.asHex(sb.toString()); // 签名
        return sign.toUpperCase();
    }

    public static class PrePay {
        private End signed;
        private String partnerKey;
        private SortedMap<String, Object> data;

        PrePay(Signer signer) {
            SortedMap<String, Object> param = new TreeMap<>();
            param.put("appid", signer.config.getAppId());
            param.put("mch_id", signer.config.getPartnerId());
            param.put("notify_url", signer.config.getPayedNotifyUrl());
            param.put("spbill_create_ip", signer.clientId);
            param.put("trade_type", signer.type.getId());
            param.put("out_trade_no", signer.outTradeNo);
            param.put("total_fee", signer.totalFee);
            String nonceStr = Random.length(32);
            param.put("nonce_str", nonceStr);
            param.put("sign", sign(signer.config.getPartnerKey(), param));

            this.data = param;
            this.signed = End.of(signer.config);
            this.signed.setNonceStr(nonceStr);
            this.partnerKey = signer.config.getPartnerKey();
        }

        public PrePay ttlMinutes(int timeoutMinutes) {
            LocalDateTime start = LocalDateTime.now();
            this.data.put("time_start", DateTime.format(start, "yyyyMMddHHmmss"));// 订单生成时间
            this.data.put("time_expire", DateTime.format(start.plusMinutes(timeoutMinutes), "yyyyMMddHHmmss"));// 订单失效时间
            return this;
        }

        public PrePay body(String body) {
            this.data.put("body", body);
            return this;
        }

        public PrePay attach(String json) {
            this.data.put("attach", json);
            return this;
        }

        @Nullable
        public End then(Function<SortedMap<String, Object>, XReader> function) {
            // 获取预支付订单ID
            XNode prepayId = null;
            try {
                prepayId = function.apply(this.data).evalNode("//prepay_id");
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            if (Objects.isNull(prepayId)) {
                return null;
            }
            this.signed.setPrepayId(prepayId.body());
            this.signed.setSign(sign(this.partnerKey, this.signed.toMap()));
            return this.signed;
        }
    }

    @Data
    @Builder
    public static class End {
        private String appId;
        private String partnerId;
        private String prepayId;
        private String packageType;
        private String nonceStr;
        private String timestamp;
        private String sign;

        static End of(Config config) {
            return End.builder()
                    .appId(config.getAppId())
                    .partnerId(config.getPartnerId())
                    .timestamp(Long.toString(System.currentTimeMillis() / 1000))
                    .packageType("Sign=WXPay")
                    .build();
        }

        Map<String, Object> toMap() {
            return Maps.<String, Object>as(new TreeMap<>())
                    .put("appid", appId)
                    .put("partnerid", partnerId)
                    .put("prepayid", prepayId)
                    .put("package", packageType)
                    .put("noncestr", nonceStr)
                    .put("timestamp", timestamp)
                    .build();
        }
    }

    public enum Type {
        APP("APP"), JS_API("JSAPI");

        @Getter
        private String id;

        Type(String id) {
            this.id = id;
        }

        public static boolean containsId(String id) {
            if (StringUtils.hasLength(id)) {
                for (Type type : values()) {
                    if (type.id.equals(id)) {
                        return true;
                    }
                }
            }
            return false;
        }

        public static boolean idEquals(String id, Type type) {
            return type.id.equals(id);
        }
    }
}
