package com.lhiot.oc.payment.api;

import com.alipay.api.AlipayApiException;
import com.alipay.api.domain.AlipayTradeAppPayModel;
import com.alipay.api.domain.AlipayTradeCancelModel;
import com.leon.microx.support.result.Tips;
import com.leon.microx.util.Jackson;
import com.lhiot.oc.payment.domain.Attach;
import com.lhiot.oc.payment.domain.PaymentLog;
import com.lhiot.oc.payment.domain.SignParam;
import com.lhiot.oc.payment.domain.enums.PayPlatformType;
import com.lhiot.oc.payment.domain.enums.PayStepType;
import com.lhiot.oc.payment.feign.BaseDataServerFeign;
import com.lhiot.oc.payment.feign.domain.PaymentSign;
import com.lhiot.oc.payment.service.PaymentLogService;
import com.lhiot.oc.payment.service.payment.AliPayUtil;
import com.lhiot.oc.payment.service.payment.PaymentProperties;
import com.lhiot.oc.payment.util.DateFormatUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Api(description = "支付接口 - 支付宝")
@RestController
@RequestMapping("/alipay")
public class AliPaymentApi {

    private final PaymentLogService paymentLogService;
    private final BaseDataServerFeign dataServerFeign;
    private AliPayUtil aliPayUtil;

    @Autowired
    public AliPaymentApi(PaymentProperties properties,
                         PaymentLogService paymentLogService, BaseDataServerFeign dataServerFeign) {

        this.paymentLogService = paymentLogService;
        this.dataServerFeign = dataServerFeign;
        this.aliPayUtil = new AliPayUtil(properties);
    }

    @ApiOperation(value = "支付 - 签名")
    @PostMapping("/sign")
    public ResponseEntity<Tips> sign(@RequestBody SignParam signParam) throws AlipayApiException {
        log.debug("===支付宝签名 start=== " + signParam);
        Tips tips = paymentLogService.validateSignParam(signParam);
        if (tips.getCode().equals("-1")) {
            return ResponseEntity.badRequest().body(tips);
        }
        //依据前端传递的支付商户简称查询支付配置信息
        ResponseEntity<PaymentSign> paymentSignResponseEntity = dataServerFeign.findPaymentSignByPaymentName(signParam.getAttach().getPaymentName());
        if(Objects.isNull(paymentSignResponseEntity)||paymentSignResponseEntity.getStatusCode().isError()){
            return ResponseEntity.badRequest().body(Tips.of(-1,"远程查询支付配置信息失败"));
        }
        PaymentSign paymentSign = paymentSignResponseEntity.getBody();
        if(Objects.isNull(paymentSign)) {
            return ResponseEntity.badRequest().body(Tips.of(-1, "未找到支付配置信息"));
        }
        if(!Objects.equals(paymentSign.getPayPlatformType(), PayPlatformType.ALIPAY.name())){
            return ResponseEntity.badRequest().body(Tips.of(-1,"支付配置信息与调用接口不匹配"));
        }
        PaymentProperties.AliPayConfig aliPayConfig = aliPayUtil.getProperties().getAliPayConfig();
        aliPayConfig.setNotifyUrl(signParam.getBackUrl());//设置回调地址
        aliPayConfig.setAppId(paymentSign.getPartnerId());//设置支付商户
        aliPayConfig.setAliPayPublicKey(paymentSign.getPartnerKey());//设置公钥
        aliPayConfig.setAliPayPrivateKey(paymentSign.getPrivateKey());//设置私钥
        aliPayConfig.setSellerId(paymentSign.getAliSellerId());//设置支付销售账户
        aliPayUtil.getProperties().setAliPayConfig(aliPayConfig);//覆盖原有配置文件中的信息

        AlipayTradeAppPayModel model = aliPayUtil.createAliPayTradeAppPayModel(signParam);
        model.setTotalAmount(AliPayUtil.fenToYuan(signParam.getFee()));//转换成元

        Tips alipaySignedBackMsg = aliPayUtil.sign(model);
        if (alipaySignedBackMsg.getCode().equals("-1")) {
            return ResponseEntity.badRequest().body(alipaySignedBackMsg);
        }


        paymentLogService.insertPaymentLog(signParam);

        return ResponseEntity.ok(alipaySignedBackMsg);
    }

    @ApiOperation(value = "支付 -回调api")
    @PostMapping("/notify")
    public ResponseEntity<Tips> payNotify(Map<String, String> params){

        log.info("========支付成功，后台回调=======");
        //Map<String, String> params = aliPayUtil.aliPayNotifyParams(request);

        String payCode = params.get("trade_no");

        //获取附加参数
        Attach attach = Jackson.object(params.get("passback_params"), Attach.class);

        //依据前端传递的支付商户简称查询支付配置信息
        ResponseEntity<PaymentSign> paymentSignResponseEntity = dataServerFeign.findPaymentSignByPaymentName(attach.getPaymentName());
        if(Objects.isNull(paymentSignResponseEntity)||paymentSignResponseEntity.getStatusCode().isError()){
            return ResponseEntity.badRequest().body(Tips.of(-1,"远程查询支付配置信息失败"));
        }
        PaymentSign paymentSign = paymentSignResponseEntity.getBody();
        if(Objects.isNull(paymentSign)) {
            return ResponseEntity.badRequest().body(Tips.of(-1, "未找到支付配置信息"));
        }
        if(!Objects.equals(paymentSign.getPayPlatformType(), PayPlatformType.ALIPAY.name())){
            return ResponseEntity.badRequest().body(Tips.of(-1,"支付配置信息与调用接口不匹配"));
        }
        PaymentProperties.AliPayConfig aliPayConfig = aliPayUtil.getProperties().getAliPayConfig();
        aliPayConfig.setAppId(paymentSign.getPartnerId());//设置支付商户
        aliPayConfig.setAliPayPublicKey(paymentSign.getPartnerKey());//设置公钥
        aliPayConfig.setAliPayPrivateKey(paymentSign.getPrivateKey());//设置私钥
        aliPayConfig.setSellerId(paymentSign.getAliSellerId());//设置支付销售账户
        aliPayUtil.getProperties().setAliPayConfig(aliPayConfig);//覆盖原有配置文件中的信息

        //计算签名
        if (aliPayUtil.verifySeller(params)) {
            //幂等处理
            PaymentLog paymentLog = paymentLogService.getPaymentLogByPayCodeAndPayStep(payCode, PayStepType.NOTIFY.name());
            if (Objects.nonNull(paymentLog)) {
                return ResponseEntity.ok(Tips.of(1,"success"));
            }

            int fee = Integer.parseInt(AliPayUtil.yuanToFen(params.get("total_amount")));

            if (paymentLog.getFee() != fee) {
                log.error("支付金额与回调金额不一致{},{}", fee, paymentLog);
                return ResponseEntity.badRequest().body(Tips.of(-1,"failure"));
            }

            //获取传达的附加参数获取用户信息
            log.info("attach:{}" + attach + "-fee:" + fee);

            paymentLog.setPayStep(PayStepType.NOTIFY);//支付步骤

            paymentLog.setPayAt(Objects.isNull(params.get("gmt_payment"))?new Timestamp(System.currentTimeMillis()):new Timestamp(DateFormatUtil.format1(params.get("gmt_payment")).getTime()));
            paymentLog.setTradeId(params.get("trade_no"));
            paymentLog.setBankType(params.get("fund_bill_list"));

            //记录日志
            paymentLogService.updatePaymentLog(paymentLog);
            return ResponseEntity.ok(Tips.of(1,"success"));
        }
        return ResponseEntity.badRequest().body(Tips.of(-1,"failure"));
    }

    @ApiOperation(value = "撤销支付宝支付")
    @PutMapping("/cancel/{payCode}")
    public ResponseEntity<Tips> cancel(@PathVariable("payCode") String payCode,@RequestParam("paymentName") String paymentName,
                                       @RequestParam("refundMemo") String refundMemo) throws AlipayApiException {
        log.info("========撤销支付宝支付成功，后台回调=======");

        //依据前端传递的支付商户简称查询支付配置信息
        ResponseEntity<PaymentSign> paymentSignResponseEntity = dataServerFeign.findPaymentSignByPaymentName(paymentName);
        if(Objects.isNull(paymentSignResponseEntity)||paymentSignResponseEntity.getStatusCode().isError()){
            return ResponseEntity.badRequest().body(Tips.of(-1,"远程查询支付配置信息失败"));
        }
        PaymentSign paymentSign = paymentSignResponseEntity.getBody();
        if(Objects.isNull(paymentSign)) {
            return ResponseEntity.badRequest().body(Tips.of(-1, "未找到支付配置信息"));
        }
        if(!Objects.equals(paymentSign.getPayPlatformType(), PayPlatformType.ALIPAY.name())){
            return ResponseEntity.badRequest().body(Tips.of(-1,"支付配置信息与调用接口不匹配"));
        }
        PaymentProperties.AliPayConfig aliPayConfig = aliPayUtil.getProperties().getAliPayConfig();
        aliPayConfig.setAppId(paymentSign.getPartnerId());//设置支付商户
        aliPayConfig.setAliPayPublicKey(paymentSign.getPartnerKey());//设置公钥
        aliPayConfig.setAliPayPrivateKey(paymentSign.getPrivateKey());//设置私钥
        aliPayConfig.setSellerId(paymentSign.getAliSellerId());//设置支付销售账户
        aliPayUtil.getProperties().setAliPayConfig(aliPayConfig);//覆盖原有配置文件中的信息

        AlipayTradeCancelModel model=new AlipayTradeCancelModel();
        model.setOutTradeNo(payCode);

        //撤销支付不需要更新支付日志
        Tips tips =aliPayUtil.cancel(model);
        return ResponseEntity.ok(tips);
    }


    @ApiOperation(value = "撤销支付宝支付 - 异步回调")
    @PostMapping("/cancel/notify")
    public ResponseEntity<String> cancelNotify(Map<String, String> params){
        log.info("========撤销支付宝支付成功，后台回调======={}",params.get("trade_no"));
        //Map<String, String> params = aliPayUtil.aliPayNotifyParams(request);
        return ResponseEntity.ok("success");
    }


    @ApiOperation(value = "支付宝退款")
    @PutMapping("/refund/{payCode}")
    public ResponseEntity<Tips> refund(@PathVariable("payCode") String payCode,
                                       @RequestParam("paymentName") String paymentName,
                                       @RequestParam("refundFee") Long refundFee,
                                       @RequestParam("refundMemo") String refundMemo) throws Exception {
        //只退一次 退款单编号就是支付编号

        ResponseEntity<PaymentSign> paymentSignResponseEntity = dataServerFeign.findPaymentSignByPaymentName(paymentName);
        if(Objects.isNull(paymentSignResponseEntity)||paymentSignResponseEntity.getStatusCode().isError()){
            return ResponseEntity.badRequest().body(Tips.of(-1,"远程查询支付配置信息失败"));
        }
        PaymentSign paymentSign = paymentSignResponseEntity.getBody();
        if(Objects.isNull(paymentSign)) {
            return ResponseEntity.badRequest().body(Tips.of(-1, "未找到支付配置信息"));
        }
        if(!Objects.equals(paymentSign.getPayPlatformType(), PayPlatformType.ALIPAY.name())){
            return ResponseEntity.badRequest().body(Tips.of(-1,"支付配置信息与调用接口不匹配"));
        }
        PaymentProperties.AliPayConfig aliPayConfig = aliPayUtil.getProperties().getAliPayConfig();
        aliPayConfig.setAppId(paymentSign.getPartnerId());//设置支付商户
        aliPayConfig.setAliPayPublicKey(paymentSign.getPartnerKey());//设置公钥
        aliPayConfig.setAliPayPrivateKey(paymentSign.getPrivateKey());//设置私钥
        aliPayConfig.setSellerId(paymentSign.getAliSellerId());//设置支付销售账户
        aliPayUtil.getProperties().setAliPayConfig(aliPayConfig);//覆盖原有配置文件中的信息
        boolean refundResult = aliPayUtil.refund(payCode,aliPayUtil.fenToYuan(refundFee),refundMemo,payCode);//退款
        if(refundResult){
            PaymentLog paymentLog=new PaymentLog();
            paymentLog.setPayCode(payCode);
            paymentLog.setPayStep(PayStepType.REFUND);//支付步骤
            //记录日志
            paymentLogService.updatePaymentLog(paymentLog,refundMemo);
            return ResponseEntity.ok(Tips.of(1,"退款成功"));
        }

        return ResponseEntity.ok(Tips.of(-1,"退款失败"));
    }

}
