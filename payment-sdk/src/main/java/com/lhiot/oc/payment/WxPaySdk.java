package com.lhiot.oc.payment;

import com.leon.microx.util.Maps;
import com.leon.microx.util.auditing.Random;
import com.leon.microx.util.xml.XNode;
import com.leon.microx.util.xml.XReader;
import com.lhiot.oc.payment.wxpay.Api;
import com.lhiot.oc.payment.wxpay.Config;
import com.lhiot.oc.payment.wxpay.Http;
import com.lhiot.oc.payment.wxpay.Signer;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * @author Leon (234239150@qq.com) created in 10:33 18.11.30
 */
@Slf4j
public class WxPaySdk {
    public static final int DEFAULT_PAY_TIMEOUT = 6;

    private Config config;
    private Http http;

    private WxPaySdk(Config config, Http http) {
        this.config = config;
        this.http = http;
    }

    public static WxPaySdk of(Config config) {
        return new WxPaySdk(config, Http.client());
    }

    public static WxPaySdk sandbox(Config config) {
        return new WxPaySdk(config, Http.sandbox());
    }

    public Signer app(String clientIp, String outTradeNo, String fee) {
        return Signer.app(config, clientIp, outTradeNo, fee);
    }

    private Signer jsApi(String clientIp, String outTradeNo, String fee) {
        return Signer.jsApi(config, clientIp, outTradeNo, fee);
    }

    // 验签
    public boolean check(String clientIp, Map<String, String> notify) {
        if (!notify.containsKey("out_trade_no")
                || !notify.containsKey("total_fee")
                || !notify.containsKey("nonce_str")
                || !notify.containsKey("sign")
                || !notify.containsKey("sign_type")) {
            return false;
        }
        String signType = notify.get("sign_type");
        if (!Signer.Type.containsId(signType)) {
            return false;
        }
        if (Signer.Type.idEquals(signType, Signer.Type.APP)) {
            return app(clientIp, notify.get("out_trade_no"), notify.get("total_fee"))
                    .verify(notify.get("nonce_str"), notify.get("sign"));
        }
        if (Signer.Type.idEquals(signType, Signer.Type.JS_API)) {
            return jsApi(clientIp, notify.get("out_trade_no"), notify.get("total_fee"))
                    .verify(notify.get("nonce_str"), notify.get("sign"));
        }
        return false;
    }

    public boolean refund(String refundId, String outTradeNo, String totalFee, String refundFee) {
        Map<String, Object> data = Maps.<String, Object>as(new TreeMap<>())
                .put("nonce_str", Random.length(32))
                .put("appid", config.getAppId())
                .put("mch_id", config.getPartnerId())
                .put("op_user_id", config.getPartnerId())
                .put("out_refund_no", refundId)
                .put("out_trade_no", outTradeNo)
                .put("total_fee", totalFee)
                .put("refund_fee", refundFee)
                .put("notify_url", config.getRefundNotifyUrl()).build();
        data.put("sign", Signer.sign(config.getPartnerKey(), data));
        return isOk(
                http.post(Api.REFUND, data, Http.ssl(config.getCertificate(), config.getPartnerId().toCharArray()))
        );
    }

    public boolean close(String outTradeNo) {
        Map<String, Object> data = Maps.<String, Object>as(new TreeMap<>())
                .put("nonce_str", Random.length(32))
                .put("appid", config.getAppId())
                .put("mch_id", config.getPartnerId())
                .put("out_trade_no", outTradeNo)
                .build();
        data.put("sign", Signer.sign(config.getPartnerKey(), data));
        return isOk(http.post(Api.CLOSE_ORDER, data));
    }

    private static boolean isOk(XReader xml) {
        XNode returnCode = xml.evalNode("//return_code");
        XNode resultCode = xml.evalNode("//result_code");
        return Objects.nonNull(returnCode)
                && "SUCCESS".equalsIgnoreCase(returnCode.body())
                && Objects.nonNull(resultCode)
                && "SUCCESS".equalsIgnoreCase(resultCode.body());
    }
}
