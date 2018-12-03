package com.lhiot.oc.payment.alipay;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeAppPayModel;
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.leon.microx.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Leon (234239150@qq.com) created in 11:34 18.11.30
 */
@Slf4j
public class Signer {
    private AlipayClient client;
    private String notifyUrl;

    public Signer(AlipayClient client, String notifyUrl) {
        this.client = client;
        this.notifyUrl = notifyUrl;
    }

    public Optional<String> sign(AlipayTradeAppPayModel model) {
        AlipayTradeAppPayRequest request = new AlipayTradeAppPayRequest();
        request.setBizModel(model);
        request.setNotifyUrl(notifyUrl);
        try {
            AlipayTradeAppPayResponse signed = client.sdkExecute(request);
            if (Objects.nonNull(signed) && Objects.nonNull(signed.getBody())) {
                return Optional.of(signed.getBody());
            }
        } catch (AlipayApiException e) {
            log.error(StringUtils.format("errCode: {}, errMsg: {}", e.getErrCode(), e.getErrMsg()), e);
        }
        return Optional.empty();
    }

    public Optional<String> sign(AlipayTradeWapPayModel model) {
        throw new UnsupportedOperationException();
    }

    public enum Type {APP, WAP}
}
