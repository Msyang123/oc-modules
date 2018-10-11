package com.lhiot.oc.payment.api;

import com.leon.microx.support.result.Tips;
import com.lhiot.oc.payment.domain.PaymentLog;
import com.lhiot.oc.payment.domain.SignParam;
import com.lhiot.oc.payment.domain.enums.PayPlatformType;
import com.lhiot.oc.payment.domain.enums.PayStepType;
import com.lhiot.oc.payment.domain.enums.SourceType;
import com.lhiot.oc.payment.feign.BaseUserServerFeign;
import com.lhiot.oc.payment.feign.domain.BalanceOperationParam;
import com.lhiot.oc.payment.feign.domain.OperationStatus;
import com.lhiot.oc.payment.service.PaymentLogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.util.Objects;

/**
 * @author
 */
@Slf4j
@RestController
@Api("鲜果币公共支付api")
@RequestMapping("/currency")
public class CurrencyPaymentApi {
    private final BaseUserServerFeign baseUserServerFeign;
    private final PaymentLogService paymentLogService;
    @Autowired
    public CurrencyPaymentApi(BaseUserServerFeign baseUserServerFeign, PaymentLogService paymentLogService) {
        this.baseUserServerFeign = baseUserServerFeign;
        this.paymentLogService = paymentLogService;
    }

    @ApiOperation(value = "鲜果币支付-充值接口")
    @ApiImplicitParam(paramType = "body", name = "signParam", dataType = "SignParam", required = true, value = "鲜果币支付传入参数")
    @PostMapping("/recharge")
    public ResponseEntity<?> currencyRecharge(@RequestBody SignParam signParam){
        ResponseEntity validateResult=this.validate(signParam);

        if(Objects.nonNull(validateResult)){
            return validateResult;
        }
        BalanceOperationParam balanceOperationParam=new BalanceOperationParam();
        balanceOperationParam.setApplicationType(signParam.getAttach().getApplicationType());
        balanceOperationParam.setMoney(signParam.getFee());
        balanceOperationParam.setOperation(OperationStatus.ADD);
        balanceOperationParam.setSourceId(signParam.getPayCode());
        balanceOperationParam.setSourceType(signParam.getMemo());
        //扣减或者充值鲜果币
        ResponseEntity responseEntity = baseUserServerFeign.updateCurrencyById(signParam.getAttach().getUserId(),balanceOperationParam);
        //如果成功 添加支付记录
        if(responseEntity.getStatusCode().isError()){
            return ResponseEntity.badRequest().body(responseEntity.getBody());
        }
        PaymentLog paymentLog = new PaymentLog();
        paymentLog.setUserId(signParam.getAttach().getUserId());
        paymentLog.setPayCode(signParam.getPayCode());
        paymentLog.setApplicationType(signParam.getAttach().getApplicationType());
        paymentLog.setSourceType(SourceType.RECHARGE);
        paymentLog.setPayPlatformType(null);//不确定支付平台
        paymentLog.setPayStep(PayStepType.NOTIFY);//支付步骤
        paymentLog.setFee(signParam.getFee());//支付金额
        paymentLog.setPayAt(new Timestamp(System.currentTimeMillis()));
        paymentLogService.insertPaymentLog(paymentLog);
        return ResponseEntity.ok().build();
    }


    @ApiOperation(value = "鲜果币支付-支付接口")
    @ApiImplicitParam(paramType = "body", name = "signParam", dataType = "SignParam", required = true, value = "鲜果币支付传入参数")
    @PostMapping("/pay")
    public ResponseEntity<?> currencyPay(@RequestBody SignParam signParam){
        ResponseEntity validateResult=this.validate(signParam);

        if(Objects.nonNull(validateResult)){
            return validateResult;
        }

        BalanceOperationParam balanceOperationParam=new BalanceOperationParam();
        balanceOperationParam.setApplicationType(signParam.getAttach().getApplicationType());
        balanceOperationParam.setMoney(signParam.getFee());
        balanceOperationParam.setOperation(OperationStatus.SUBTRACT);
        balanceOperationParam.setSourceId(signParam.getPayCode());
        balanceOperationParam.setSourceType(signParam.getMemo());
        //扣减或者充值鲜果币
        ResponseEntity responseEntity = baseUserServerFeign.updateCurrencyById(signParam.getAttach().getUserId(),balanceOperationParam);
        //如果成功 添加支付记录
        if(responseEntity.getStatusCode().isError()){
            return ResponseEntity.badRequest().body(responseEntity.getBody());
        }
        PaymentLog paymentLog = new PaymentLog();
        paymentLog.setUserId(signParam.getAttach().getUserId());
        paymentLog.setPayCode(signParam.getPayCode());
        paymentLog.setApplicationType(signParam.getAttach().getApplicationType());
        paymentLog.setSourceType(signParam.getAttach().getSourceType());
        paymentLog.setPayPlatformType(PayPlatformType.BALANCE);
        paymentLog.setPayStep(PayStepType.NOTIFY);//支付步骤
        paymentLog.setFee(signParam.getFee());//支付金额
        paymentLog.setPayAt(new Timestamp(System.currentTimeMillis()));
        paymentLogService.insertPaymentLog(paymentLog);
        return ResponseEntity.ok().build();
    }


    @ApiOperation(value = "鲜果币支付-支付退款接口")
    @ApiImplicitParam(paramType = "body", name = "signParam", dataType = "SignParam", required = true, value = "鲜果币支付传入参数")
    @PostMapping("/refund")
    public ResponseEntity<?> currencyRefund(@RequestBody SignParam signParam){
        ResponseEntity validateResult=this.validate(signParam);

        if(Objects.nonNull(validateResult)){
            return validateResult;
        }

        if(Objects.nonNull(paymentLogService.getPaymentLogByPayCodeAndPayStep(signParam.getPayCode(),PayStepType.NOTIFY.name()))){
            return ResponseEntity.badRequest().body(Tips.of("-1", "此笔交易已返回鲜果币，请勿重复发起退款"));
        }
        BalanceOperationParam balanceOperationParam=new BalanceOperationParam();
        balanceOperationParam.setApplicationType(signParam.getAttach().getApplicationType());
        balanceOperationParam.setMoney(signParam.getFee());
        balanceOperationParam.setOperation(OperationStatus.ADD);//添加 返回鲜果币
        balanceOperationParam.setSourceId(signParam.getPayCode());
        balanceOperationParam.setSourceType(signParam.getMemo());
        //扣减或者充值鲜果币
        ResponseEntity responseEntity = baseUserServerFeign.updateCurrencyById(signParam.getAttach().getUserId(),balanceOperationParam);
        //如果成功 添加支付记录
        if(responseEntity.getStatusCode().is2xxSuccessful()){
            PaymentLog paymentLog = new PaymentLog();
            paymentLog.setPayCode(signParam.getPayCode());
            paymentLog.setPayStep(PayStepType.REFUND);//支付步骤
            paymentLogService.updatePaymentLog(paymentLog);
        }
        return responseEntity;
    }

    @Nullable
    private ResponseEntity validate(SignParam signParam){
        if(Objects.isNull(signParam)){
            return ResponseEntity.badRequest().body(Tips.of("-1", "支付传递参数为空"));
        }
        if(Objects.isNull(signParam.getAttach())){
            return ResponseEntity.badRequest().body(Tips.of("-1", "鲜果币充值支付传递附加参数为空"));
        }
        ResponseEntity baseUser = baseUserServerFeign.findUserById(signParam.getAttach().getUserId());
        if(Objects.isNull(baseUser)||baseUser.getStatusCode().isError()){
            return ResponseEntity.badRequest().body(Tips.of("-1", "基础用户不存在"));
        }
        if (Objects.isNull(signParam.getFee())) {
            return ResponseEntity.badRequest().body(Tips.of("-1", "支付金额为空"));
        }
        if (signParam.getFee()<=0) {
            return ResponseEntity.badRequest().body(Tips.of("-1", "支付金额不能小于0"));
        }

        return null;
    }
}
