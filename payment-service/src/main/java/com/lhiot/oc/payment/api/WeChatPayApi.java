package com.lhiot.oc.payment.api;

import com.leon.microx.pay.Payments;
import com.leon.microx.pay.model.SignAttrs;
import com.leon.microx.pay.type.TradeType;
import com.leon.microx.web.swagger.ApiParamType;
import com.lhiot.oc.payment.feign.PaymentConfig;
import com.lhiot.oc.payment.model.WxPayModel;
import com.lhiot.oc.payment.service.PaymentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Leon (234239150@qq.com) created in 9:24 18.11.29
 */
@Slf4j
@Validated
@RestController
@Api("支付接口 - 微信")
@RequestMapping("/we-chat")
public class WeChatPayApi {

    private PaymentService service;

    public WeChatPayApi(PaymentService service) {
        this.service = service;
    }

    @PostMapping("/app/sign")
    @ApiOperation(value = "支付 - APP签名", response = Map.class)
    public ResponseEntity appSign(@Valid @RequestBody WxPayModel wxPay) {
        PaymentConfig config = service.findPaymentConfig(wxPay.getConfigName());
        if (Objects.isNull(config)) {
            return ResponseEntity.badRequest().body("商户配置名错误，无法加载支付配置");
        }
        SignAttrs signAttrs = service.ready(wxPay, TradeType.WX_APP);
        return Payments.wx(config.getAppId(), config.getAppSecretKey(), config.getMerchantId(), config.getMerchantSecretKey())
                .order()
                .app(signAttrs, wxPay.getClientIp(), wxPay.getBackUrl())
                .<ResponseEntity>map(ResponseEntity::ok)
                .orElse(ResponseEntity.badRequest().body("签名失败"));
    }

    @PostMapping("/js-api/sign")
    @ApiOperation(value = "支付 - JS API签名", response = Map.class)
    public ResponseEntity jsApiSign(@Valid @RequestBody WxPayModel wxPay) {
        PaymentConfig config = service.findPaymentConfig(wxPay.getConfigName());
        if (Objects.isNull(config)) {
            return ResponseEntity.badRequest().body("商户配置名错误，无法加载支付配置");
        }
        SignAttrs signAttrs = service.ready(wxPay, TradeType.WX_JS_API);
        return Payments.wx(config.getAppId(), config.getAppSecretKey(), config.getMerchantId(), config.getMerchantSecretKey())
                .order()
                .jsApi(signAttrs, wxPay.getOpenid(), wxPay.getClientIp(), wxPay.getBackUrl())
                .<ResponseEntity>map(ResponseEntity::ok)
                .orElse(ResponseEntity.badRequest().body("签名失败"));
    }

    @PostMapping("/payed/{outTradeNo}/verification")
    @ApiOperation("支付完成 - 验签")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = ApiParamType.PATH, name = "outTradeNo", value = "支付ID", dataType = "Long", required = true),
            @ApiImplicitParam(paramType = ApiParamType.BODY, name = "notifiedParameters", value = "第三方回调验签参数", dataTypeClass = HashMap.class, required = true)
    })
    public ResponseEntity verify(@PathVariable("outTradeNo") Long outTradeNo, @NotEmpty @Size(min = 2) @RequestBody Map<String, Object> notifiedParameters) {
        PaymentConfig config = service.findPaymentConfig(outTradeNo);
        if (Objects.isNull(config)) {
            return ResponseEntity.badRequest().body("商户配置名错误，无法加载支付配置");
        }
        return Payments.wx(config.getAppId(), config.getAppSecretKey(), config.getMerchantId(), config.getMerchantSecretKey())
                .order().verify(notifiedParameters)
                ? ResponseEntity.ok().build()
                : ResponseEntity.badRequest().body("验签失败！");
    }

}
