package com.lhiot.oc.payment.api;

import com.leon.microx.pay.Payments;
import com.leon.microx.pay.model.SignAttrs;
import com.leon.microx.web.swagger.ApiParamType;
import com.lhiot.oc.payment.entity.Record;
import com.lhiot.oc.payment.feign.PaymentConfig;
import com.lhiot.oc.payment.model.AliPayModel;
import com.lhiot.oc.payment.service.PaymentService;
import com.lhiot.oc.payment.type.PayStep;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Validated
@RestController
@Api("支付接口 - 蚂蚁金服")
@RequestMapping("/ant-financial")
public class AntFinancialApi {

    private PaymentService service;

    public AntFinancialApi(PaymentService service) {
        this.service = service;
    }

    @Transactional
    @PostMapping("/app/sign")
    @ApiOperation(value = "App支付 - 签名", notes = "本服务支付宝方面 目前只支持原生APP支付。", response = String.class)
    public ResponseEntity appSign(@Valid @RequestBody AliPayModel aliPay) {
        PaymentConfig config = service.findPaymentConfig(aliPay.getConfigName());
        if (Objects.isNull(config)) {
            return ResponseEntity.badRequest().body("商户配置名错误，无法加载支付配置");
        }
        SignAttrs signAttrs = service.ready(aliPay);
        return Payments.ali(config.getAppId(), config.getAppSecretKey(), config.getThirdPartyKey())
                .order(config.getMerchantId())
                .app(signAttrs, aliPay.getBackUrl())
                .<ResponseEntity>map(ResponseEntity::ok)
                .orElse(ResponseEntity.badRequest().body("签名失败"));
    }

    @PostMapping("/paid/{outTradeNo}/verification")
    @ApiOperation(value = "支付完成 - 验签", notes = "第三方回调参数请解析为Map后原样传递到此接口完成签名校验")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = ApiParamType.PATH, name = "outTradeNo", value = "支付ID", dataType = "Long", required = true),
            @ApiImplicitParam(paramType = ApiParamType.BODY, name = "notifiedParameters", value = "第三方回调验签参数", dataTypeClass = HashMap.class, required = true)
    })
    public ResponseEntity verify(@PathVariable("outTradeNo") Long outTradeNo, @NotEmpty @Size(min = 2) @RequestBody Map<String, String> notifiedParameters) {
        Record record = service.record(outTradeNo);
        if (Objects.isNull(record)) {
            return ResponseEntity.badRequest().body("支付单号错误！验签失败。");
        }
        if (Objects.equals(PayStep.PAID, record.getPayStep())) {
            return ResponseEntity.badRequest().body("支付订单为完成状态，请勿重复操作。");
        }
        PaymentConfig config = service.findPaymentConfig(record.getConfigName());
        if (Objects.isNull(config)) {
            return ResponseEntity.badRequest().body("商户配置名错误，无法加载支付配置");
        }
        return Payments.ali(config.getAppId(), config.getAppSecretKey(), config.getThirdPartyKey())
                .order(config.getMerchantId())
                .verify(notifiedParameters)
                ? ResponseEntity.ok().build()
                : ResponseEntity.badRequest().body("验签失败！");
    }

    @DeleteMapping("/paid/{outTradeNo}")
    @ApiOperation(value = "支付完成 - 撤销支付", notes = "【】一般用于回调异常】 如果已支付成功，则第三方自动退款，如果未支付，则第三方取消本次支付。")
    @ApiImplicitParam(paramType = ApiParamType.PATH, name = "outTradeNo", value = "支付ID", dataType = "Long", required = true)
    public ResponseEntity cancel(@PathVariable("outTradeNo") Long outTradeNo) {
        PaymentConfig config = service.findPaymentConfig(outTradeNo);
        if (Objects.isNull(config)) {
            return ResponseEntity.badRequest().body("商户配置名错误，无法加载支付配置");
        }
        return Payments.ali(config.getAppId(), config.getAppSecretKey(), config.getThirdPartyKey())
                .cancel(String.valueOf(outTradeNo))
                ? ResponseEntity.ok().build()
                : ResponseEntity.badRequest().body("撤销失败");
    }
}
