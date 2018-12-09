package com.lhiot.oc.payment.api;

import com.leon.microx.pay.Payments;
import com.leon.microx.pay.type.TradeType;
import com.leon.microx.web.swagger.ApiParamType;
import com.lhiot.oc.payment.entity.Record;
import com.lhiot.oc.payment.feign.PaymentConfig;
import com.lhiot.oc.payment.model.RefundModel;
import com.lhiot.oc.payment.service.PaymentService;
import com.lhiot.oc.payment.service.RefundService;
import com.lhiot.oc.payment.type.PayStep;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

@Slf4j
@Validated
@RestController
@Api("支付退款接口")
public class RefundApi {

    private PaymentService paymentService;
    private RefundService refundService;
    private ResourceLoader resourceLoader;

    public RefundApi(PaymentService paymentService, RefundService refundService, ResourceLoader resourceLoader) {
        this.paymentService = paymentService;
        this.refundService = refundService;
        this.resourceLoader = resourceLoader;
    }

    @PostMapping("/payed/{outTradeNo}/refunds")
    @ApiOperation("支付 - 退款（支持部分、多次退款）")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = ApiParamType.PATH, name = "outTradeNo", value = "支付ID", dataType = "Long", required = true),
            @ApiImplicitParam(paramType = ApiParamType.BODY, name = "refund", value = "第三方回调验签参数", dataType = "RefundModel", required = true)
    })
    public ResponseEntity refund(@PathVariable("outTradeNo") Long outTradeNo, @Valid @RequestBody RefundModel refund) {
        Record record = paymentService.record(outTradeNo);
        if (Objects.isNull(record)) {
            return ResponseEntity.badRequest().body("支付单号错误！退款失败。");
        }

        if (!record.getPayStep().equals(PayStep.PAYED)) {
            return ResponseEntity.badRequest().body("第三方尚未通知完成支付，请稍后再尝试。");
        }

        if (!refundService.canRefund(record, refund)){
            return ResponseEntity.badRequest().body("退款金额不符。");
        }

        if (Objects.isNull(record.getSignAt()) && record.getTradeType().equals(TradeType.OTHER_PAY)) {
            boolean completed = refundService.balanceRefund(record, refund);
            return completed ? ResponseEntity.ok().build() : ResponseEntity.badRequest().body("退还余额失败！");
        }

        PaymentConfig config = paymentService.findPaymentConfig(record.getConfigName());
        if (Objects.isNull(config)) {
            return ResponseEntity.badRequest().body("商户配置名错误，无法加载支付配置");
        }

        long outRefundNo = refundService.createRefund(record, refund);

        if (record.getTradeType().equals(TradeType.ALI_APP)) {
            return Payments.ali(config.getAppId(), config.getAppSecretKey(), config.getThirdPartyKey())
                    .refund(String.valueOf(outTradeNo), String.valueOf(outRefundNo), refund.getFee(), refund.getNotifyUrl(), refund.getReason())
                    ? ResponseEntity.ok().build()
                    : ResponseEntity.badRequest().body("支付宝退款失败");
        }

        if (record.getTradeType().equals(TradeType.WX_APP) || record.getTradeType().equals(TradeType.WX_JS_API)) {
            InputStream certInput = this.loadCertInput(config.getThirdPartyKey());
            if (Objects.isNull(certInput)) {
                return ResponseEntity.badRequest().body("商户退款密钥错误！退款失败");
            }
            return Payments.wx(config.getAppId(), config.getAppSecretKey(), config.getMerchantId(), config.getMerchantSecretKey())
                    .refund(certInput)
                    .send(String.valueOf(outTradeNo), String.valueOf(outRefundNo), refund.getFee(), refund.getFee(), refund.getNotifyUrl())
                    ? ResponseEntity.ok().build()
                    : ResponseEntity.badRequest().body("微信退款失败");
        }
        return ResponseEntity.badRequest().body("交易类型错误！");
    }

    @PutMapping("/refunds/{refundId}/completed")
    @ApiOperation("修改退款单为完成状态")
    public ResponseEntity completed(@PathVariable("refundId") Long refundId) {
        boolean updated = refundService.refundCompleted(refundId);
        return updated ? ResponseEntity.ok().build() : ResponseEntity.badRequest().body("修改失败");
    }

    // 微信有密退款 - 读取pkcs12文件
    @Nullable
    private InputStream loadCertInput(String url) {
        try {
            Resource cert = resourceLoader.getResource(url);
            if (cert.exists()) {
                return cert.getInputStream();
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }
}
