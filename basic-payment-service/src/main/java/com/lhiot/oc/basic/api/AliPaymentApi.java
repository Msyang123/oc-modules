package com.lhiot.oc.basic.api;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeAppPayModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.leon.microx.common.wrapper.Tips;
import com.leon.microx.util.Jackson;
import com.lhiot.oc.basic.domain.Attach;
import com.lhiot.oc.basic.domain.PaymentLog;
import com.lhiot.oc.basic.service.PaymentLogService;
import com.lhiot.oc.basic.service.PaymentService;
import com.lhiot.oc.basic.service.payment.AliPayUtil;
import com.lhiot.oc.basic.service.payment.PaymentProperties;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Api(description = "支付接口 - 支付宝")
@RestController
@RequestMapping("/alipay")
public class AliPaymentApi {

    private final PaymentService paymentService;
    private final PaymentLogService paymentLogService;
    private AliPayUtil aliPayUtil;

    @Autowired
    public AliPaymentApi(PaymentProperties properties, AlipayClient alipayClient,
                         PaymentService paymentService, PaymentLogService paymentLogService) {

        this.paymentLogService = paymentLogService;
        this.aliPayUtil = new AliPayUtil(properties, alipayClient);
        this.paymentService = paymentService;
    }

    @ApiOperation(value = "支付 - 签名")
    @PostMapping("/sign")
    public ResponseEntity<Tips> sign(@RequestBody SignParam signParam) throws JsonProcessingException, AlipayApiException {
        log.debug("===支付宝签名 start=== " + signParam);
        //memo:"水果熟了 - 鲜果师商城用户充值"
        Tips tips = paymentService.validateSignParam(signParam);
        if (tips.getCode().equals("-1")) {
            return ResponseEntity.badRequest().body(tips);
        }
        AlipayTradeAppPayModel model = aliPayUtil.createAliPayTradeAppPayModel(signParam);
        model.setTotalAmount(AliPayUtil.fenToYuan(signParam.getFee()));//转换成元

        Tips alipaySignedBackMsg = aliPayUtil.sign(model);
        if (alipaySignedBackMsg.getCode().equals("-1")) {
            return ResponseEntity.badRequest().body(alipaySignedBackMsg);
        }
        paymentService.signSavePaymentLog(signParam);

        return ResponseEntity.ok(alipaySignedBackMsg);
    }

    @ApiOperation(value = "支付 - 异步回调")
    @PostMapping("/notify")
    public ResponseEntity<String> notify(HttpServletRequest request) throws Exception {

        log.info("========支付成功，后台回调=======");
        Map<String, String> params = aliPayUtil.aliPayNotifyParams(request);

        String orderCode = params.get("trade_no");
        //计算签名
        if (aliPayUtil.verifySeller(params)) {
            //幂等处理
            PaymentLog paymentLog = paymentLogService.getPaymentLogByCode(orderCode);
            if (Objects.nonNull(paymentLog) && Objects.equals(paymentLog.getPayStep(), "paid")) {
                return ResponseEntity.ok("success");
            }

            int fee = Integer.parseInt(AliPayUtil.yuanToFen(params.get("total_amount")));

            if (paymentLog.getPayFee() != fee) {
                log.error("支付金额与回调金额不一致{},{}", fee, paymentLog);
                return ResponseEntity.ok("failure");
            }
            Attach attach = Jackson.object(params.get("passback_params"), Attach.class);
            //获取传达的附加参数获取用户信息
            log.info("attach:{}" + attach + "-fee:" + fee);

            paymentLog.setPayStep("paid");//支付步骤：sign-签名成功 paid-支付成功
            paymentLog.setPayAt(new Timestamp(DateFormatUtil.format1(params.get("gmt_payment")).getTime()));
            paymentLog.setTradeId(params.get("trade_no"));
            paymentLog.setBankType(params.get("fund_bill_list"));

            //处理业务
            paymentService.notifyUpdate(attach,paymentLog);
            //记录日志
            paymentLogService.updatePaymentLog(paymentLog);
            //广播订单支付成功true,   "success" : "failure"
            return ResponseEntity.ok("success");
        }
        return ResponseEntity.ok("failure");
    }
}
