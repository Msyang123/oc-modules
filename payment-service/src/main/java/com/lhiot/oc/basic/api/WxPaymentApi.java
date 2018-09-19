package com.lhiot.oc.basic.api;


import com.leon.microx.support.result.Tips;
import com.leon.microx.util.Jackson;
import com.leon.microx.util.xml.XNode;
import com.leon.microx.util.xml.XReader;
import com.lhiot.oc.basic.domain.Attach;
import com.lhiot.oc.basic.domain.PaymentLog;
import com.lhiot.oc.basic.domain.SignParam;
import com.lhiot.oc.basic.service.PaymentLogService;
import com.lhiot.oc.basic.service.PaymentService;
import com.lhiot.oc.basic.service.payment.PaymentProperties;
import com.lhiot.oc.basic.service.payment.WeChatUtil;
import com.lhiot.oc.basic.util.DateFormatUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author
 */
@Slf4j
@RestController
@Api("微信公共支付api")
@RequestMapping("/wxpayment")
public class WxPaymentApi {
    private final PaymentService paymentService;
    private final PaymentLogService paymentLogService;
    private final WeChatUtil weChatUtil;
    final private RabbitTemplate rabbit;

    @Autowired
    public WxPaymentApi(PaymentService paymentService, PaymentProperties properties,
                        PaymentLogService paymentLogService, RabbitTemplate rabbit) {
        this.paymentService = paymentService;
        this.weChatUtil = new WeChatUtil(properties);
        this.paymentLogService = paymentLogService;
        this.rabbit = rabbit;
    }

    /**
     * 获取请求ip地址
     */
    private String getRemoteAddr(HttpServletRequest request) {
        if ("nginx".equals(weChatUtil.getProperties().getWeChatPay().getProxy())) {
            return request.getHeader("X-Real-IP");
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/sign")
    @ApiOperation(value = "微信支付签名", response = Tips.class)
    public ResponseEntity<Tips> sign(HttpServletRequest request, @RequestBody SignParam signParam) {
        //memo:"水果熟了 - 鲜果师商城用户充值"
        Tips backMsg = paymentService.validateSignParam(signParam);
        if(backMsg.getCode().equals("-1")){
            return ResponseEntity.badRequest().body(backMsg);
        }
        //签名
        Tips wxSignBackMsg = weChatUtil.wxCreateSign(
                getRemoteAddr(request),
                signParam.getOpenid(), signParam.getFee(),
                request.getHeader("user-agent"),
                signParam.getOrderCode(),
                signParam.getMemo(),
                Jackson.json(signParam.getAttach()),
                weChatUtil);
        if (wxSignBackMsg.getCode().equals("-1")) {
            return ResponseEntity.badRequest().body(wxSignBackMsg);
        }
        log.info("=================微信预支付内部系统业务处理===============");

        paymentService.signSavePaymentLog(signParam);
        //返回签名结果
        return ResponseEntity.ok(wxSignBackMsg);
    }

    @PostMapping("/notify")
    @ApiOperation(value = "微信支付回调", response = String.class)
    public ResponseEntity<String> notify(HttpServletRequest request) throws Exception {
        log.info("========支付成功，后台回调=======");
        XReader xpath = weChatUtil.getParametersByWeChatCallback(request);
        String resultCode = xpath.evalNode("//result_code").body();
        //获取签名的单号
        String orderCode = xpath.evalNode("//out_trade_no").body();
        List<XNode> nodes = xpath.evalNodes("//xml/*");
        SortedMap<Object, Object> parameters = new TreeMap();
        for (XNode node : nodes) {
            parameters.put(node.tegName(), node.body());
        }
        //计算签名
        String signResult = weChatUtil.createSign(weChatUtil.getProperties().getWeChatPay().getLhiot().getPartnerKey(), parameters);
        log.info("signResult:" + signResult);
        log.info("urlsign:" + xpath.evalNode("//sign"));

        if ("SUCCESS".equalsIgnoreCase(resultCode) && Objects.equals(signResult, xpath.evalNode("//sign").body())) {
            //幂等处理
            PaymentLog paymentLog = paymentLogService.getPaymentLogByCode(orderCode);
            if (Objects.nonNull(paymentLog) && Objects.equals(paymentLog.getPayStep(), "paid")) {
                return ResponseEntity.ok("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<xml><return_code><![CDATA[SUCCESS]]></return_code>"
                        + "<return_msg><![CDATA[OK]]></return_msg></xml>");
            }
            int fee = Integer.parseInt(xpath.evalNode("//total_fee").body());
            if(paymentLog.getPayFee()!=fee){
                log.error("支付金额与回调金额不一致{},{}",fee,paymentLog);
                return ResponseEntity.ok().build();
            }
            Attach attach = Jackson.object(xpath.evalNode("//attach").body(), Attach.class);

            //获取传达的附加参数获取用户信息
            log.info("attach:" + attach + "-fee:" + fee);

            paymentLog.setPayStep("paid");//支付步骤：sign-签名成功 paid-支付成功
            paymentLog.setPayAt(new Timestamp(DateFormatUtil.convertPayTime(xpath.evalNode("//time_end").body(), "yyyyMMddHHmmss").getTime()));
            paymentLog.setTradeId(xpath.evalNode("//transaction_id").body());
            paymentLog.setBankType(xpath.evalNode("//bank_type").body());

            //处理业务
            paymentService.notifyUpdate(attach,paymentLog);

            //记录日志
            paymentLogService.updatePaymentLog(paymentLog);
            //XXXTODO 发送到队列，发送模板消息以及计算提成
            //广播订单支付成功true, "success"
            return ResponseEntity.ok("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<xml><return_code><![CDATA[SUCCESS]]></return_code>"
                    + "<return_msg><![CDATA[OK]]></return_msg></xml>");
        }
        return ResponseEntity.ok().build();
    }


    @ApiOperation(value = "微信退款")
    @GetMapping("/refund")
    public ResponseEntity<Tips> refund(@RequestParam("orderCode") String orderCode, @RequestParam("totalFee") int totalFee,@RequestParam("refundFee") int refundFee) {
        //只退一次 退款单编号就是订单编号
        weChatUtil.refund(orderCode,totalFee,refundFee);
        return ResponseEntity.ok(Tips.of(1,"退款成功"));
    }

    @ApiOperation("取消微信支付处理")
    @ApiImplicitParam(paramType = "query", name = "orderCode", value = "支付code", required = true, dataType = "String")
    @PutMapping("/cancel")
    public ResponseEntity<Tips> cancelPaying(@RequestParam("orderCode") String orderCode) {
        weChatUtil.cancel(orderCode);
        return ResponseEntity.ok(Tips.of(1,"取消微信支付成功"));
    }
}
