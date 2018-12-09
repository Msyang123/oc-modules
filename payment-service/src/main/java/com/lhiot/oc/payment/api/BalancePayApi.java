package com.lhiot.oc.payment.api;

import com.leon.microx.web.result.Id;
import com.lhiot.oc.payment.model.BalancePayModel;
import com.lhiot.oc.payment.service.PaymentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Slf4j
@Validated
@RestController
@Api("支付接口 - 余额支付")
@RequestMapping("/balance")
public class BalancePayApi {

    private PaymentService service;

    public BalancePayApi(PaymentService service) {
        this.service = service;
    }

    @PostMapping("/payments")
    @ApiOperation(value = "余额支付", response = Id.class)
    public ResponseEntity balancePay(@Valid @RequestBody BalancePayModel balancePay) {
        long outTradeId = service.balancePay(balancePay);
        if (outTradeId == 0) {
            return ResponseEntity.badRequest().body("余额支付失败");
        }
        return ResponseEntity.ok().body(Id.of(outTradeId));
    }
}
