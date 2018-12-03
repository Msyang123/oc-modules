package com.lhiot.oc.payment;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConstants;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeAppPayModel;
import com.alipay.api.domain.AlipayTradeCancelModel;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeCancelRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeCancelResponse;
import com.leon.microx.util.StringUtils;
import com.lhiot.oc.payment.alipay.Config;
import com.lhiot.oc.payment.alipay.Signer;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Leon (234239150@qq.com) created in 10:32 18.11.30
 */
@Slf4j
public class AliPaySdk {
    private static final String ALI_PAY_GATEWAY_URL = "https://openapi.alipay.com/gateway.do";

    private static final String DEFAULT_CHARSET_UTF8 = AlipayConstants.CHARSET_UTF8;

    private static final String ALI_PAY_SIGN_TYPE = AlipayConstants.SIGN_TYPE_RSA2;

    public static final String DEFAULT_PAY_TIMEOUT = "6m";

    private Config config;
    private AlipayClient sdk;

    private AliPaySdk(Config config) {
        this.config = config;
        this.sdk = new DefaultAlipayClient(
                ALI_PAY_GATEWAY_URL,
                config.getAppId(),
                config.getAppPrivateKey(),
                AlipayConstants.FORMAT_JSON,
                DEFAULT_CHARSET_UTF8,
                config.getAliPublicKey(),
                ALI_PAY_SIGN_TYPE
        );
    }

    public static AliPaySdk of(Config config) {
        return new AliPaySdk(config);
    }

    public Optional<String> sign(Signer.Type type, String outTradeNo, String memo, String fee) {
        if (type.equals(Signer.Type.APP)) {
            AlipayTradeAppPayModel model = new AlipayTradeAppPayModel();
            model.setSellerId(config.getMerchantId());
            model.setOutTradeNo(outTradeNo);
            model.setBody(memo);
            model.setSubject(memo);
            model.setTimeoutExpress(DEFAULT_PAY_TIMEOUT);
            model.setProductCode("QUICK_MSECURITY_PAY");
            model.setTotalAmount(fee);
            return new Signer(this.sdk, config.getPayedNotifyUrl()).sign(model);
        }
        if (type.equals(Signer.Type.WAP)) {
            throw new UnsupportedOperationException();
        }
        return Optional.empty();
    }

    // 验签通过, 同一商户, 同一应用
    public boolean check(Map<String, String> notify) {
        boolean signVerified;
        try {
            signVerified = AlipaySignature.rsaCheckV1(notify, config.getAliPublicKey(), DEFAULT_CHARSET_UTF8, ALI_PAY_SIGN_TYPE);
        } catch (AlipayApiException e) {
            return false;
        }
        return signVerified
                && Objects.equals(config.getMerchantId(), notify.get("seller_id"))
                && Objects.equals(config.getAppId(), notify.get("app_id"));
    }

    public boolean refund(String outTradeNo, String fee) {
        return refund(outTradeNo, fee, null);
    }

    public boolean refund(String outTradeNo, String fee, String reason) {
        return refund(null, outTradeNo, fee, reason);
    }

    public boolean refund(String refundId, String outTradeNo, String refundFee, String reason) {
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        AlipayTradeRefundModel model = new AlipayTradeRefundModel();
        model.setOutTradeNo(outTradeNo);
        model.setRefundAmount(refundFee);
        model.setRefundReason(reason);
        model.setOutRequestNo(refundId);
        request.setBizModel(model);
        try {
            return sdk.sdkExecute(request).isSuccess();
        } catch (AlipayApiException e) {
            log.error(StringUtils.format("errCode: {}, errMsg: {}", e.getErrCode(), e.getErrMsg()), e);
            return false;
        }
    }

    public String cancel(String outTradeNo) {
        AlipayTradeCancelModel model = new AlipayTradeCancelModel();
        model.setOutTradeNo(outTradeNo);
        AlipayTradeCancelRequest request = new AlipayTradeCancelRequest();
        request.setBizModel(model);
        request.setNotifyUrl(config.getCancelNotifyUrl());
        try {
            AlipayTradeCancelResponse cancelled = sdk.sdkExecute(request);
            if (Objects.nonNull(cancelled) && Objects.nonNull(cancelled.getBody())) {
                return cancelled.getBody();
            }
        } catch (AlipayApiException e) {
            log.error(StringUtils.format("errCode: {}, errMsg: {}", e.getErrCode(), e.getErrMsg()), e);
        }
        return null;
    }
}
