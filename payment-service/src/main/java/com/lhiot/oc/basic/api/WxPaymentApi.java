package com.lhiot.oc.delivery.api;


import com.leon.microx.support.result.Tips;
import com.leon.microx.util.Jackson;
import com.leon.microx.util.xml.XNode;
import com.leon.microx.util.xml.XReader;
import com.lhiot.oc.delivery.domain.Attach;
import com.lhiot.oc.delivery.domain.PaymentLog;
import com.lhiot.oc.delivery.domain.SignParam;
import com.lhiot.oc.delivery.domain.enums.PayPlatformType;
import com.lhiot.oc.delivery.domain.enums.PayStepType;
import com.lhiot.oc.delivery.feign.BaseDataServerFeign;
import com.lhiot.oc.delivery.feign.domain.PaymentSign;
import com.lhiot.oc.delivery.service.PaymentLogService;
import com.lhiot.oc.delivery.service.payment.PaymentProperties;
import com.lhiot.oc.delivery.service.payment.WeChatUtil;
import com.lhiot.oc.delivery.util.DateFormatUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
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
    private final PaymentLogService paymentLogService;
    private final WeChatUtil weChatUtil;
    private final BaseDataServerFeign dataServerFeign;
    private final ResourceLoader resourceLoader;

    @Autowired
    public WxPaymentApi( PaymentProperties properties,
                        PaymentLogService paymentLogService, BaseDataServerFeign dataServerFeign, ResourceLoader resourceLoader) {
        this.weChatUtil = new WeChatUtil(properties);
        this.paymentLogService = paymentLogService;
        this.dataServerFeign = dataServerFeign;
        this.resourceLoader = resourceLoader;
    }

    /**
     * 获取请求ip地址
     */
    private String getRemoteAddr(HttpServletRequest request) {
        if ("nginx".equals(weChatUtil.getProperties().getWeChatPayConfig().getProxy())) {
            return request.getHeader("X-Real-IP");
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/sign")
    @ApiOperation(value = "微信支付签名", response = Tips.class)
    public ResponseEntity<Tips> sign(HttpServletRequest request, @RequestBody SignParam signParam) {
        //memo:"水果熟了 - 鲜果师商城用户充值"
        Tips backMsg = paymentLogService.validateSignParam(signParam);
        if(backMsg.getCode().equals("-1")){
            return ResponseEntity.badRequest().body(backMsg);
        }
        //依据前端传递的支付商户简称查询支付配置信息
        ResponseEntity<PaymentSign> paymentSignResponseEntity = dataServerFeign.findPaymentSignByPaymentName(signParam.getAttach().getPaymentName());
        if(Objects.isNull(paymentSignResponseEntity)||paymentSignResponseEntity.getStatusCode().isError()){
            return ResponseEntity.badRequest().body(Tips.of(-1,"未找到支付配置信息"));
        }
        PaymentSign paymentSign = paymentSignResponseEntity.getBody();
        if(!Objects.equals(paymentSign.getPayPlatformType(), PayPlatformType.WEIXIN.name())){
            return ResponseEntity.badRequest().body(Tips.of(-1,"支付配置信息与调用接口不匹配"));
        }
        PaymentProperties.WeChatPayConfig weChatPayConfig = weChatUtil.getProperties().getWeChatPayConfig();
        weChatPayConfig.setNotifyUrl(signParam.getBackUrl());//设置回调地址
        weChatPayConfig.setPartnerId(paymentSign.getPartnerId());//设置支付商户
        weChatPayConfig.setPartnerKey(paymentSign.getPartnerKey());
        weChatUtil.getProperties().setWeChatPayConfig(weChatPayConfig);//覆盖原有配置文件中的信息
        //签名
        Tips wxSignBackMsg = weChatUtil.wxCreateSign(
                getRemoteAddr(request),
                signParam.getOpenid(),
                signParam.getAppid(),
                signParam.getFee(),
                request.getHeader("user-agent"),
                signParam.getPayCode(),
                signParam.getMemo(),
                Jackson.json(signParam.getAttach()),
                weChatUtil);
        if (wxSignBackMsg.getCode().equals("-1")) {
            return ResponseEntity.badRequest().body(wxSignBackMsg);
        }
        log.info("=================微信预支付内部系统业务处理===============");
        paymentLogService.insertPaymentLog(signParam);
        //返回签名结果
        return ResponseEntity.ok(wxSignBackMsg);
    }

    @PostMapping("/notify")
    @ApiOperation(value = "微信支付回调", response = String.class)
    public ResponseEntity<Tips> notify(String backMsg){
        log.info("========支付成功，后台回调=======");
        //XReader xpath = weChatUtil.getParametersByWeChatCallback(request);
        XReader xpath =XReader.of(backMsg);
        String resultCode = xpath.evalNode("//result_code").body();
        //获取签名的单号
        String payCode = xpath.evalNode("//out_trade_no").body();
        List<XNode> nodes = xpath.evalNodes("//xml/*");
        SortedMap<Object, Object> parameters = new TreeMap();
        for (XNode node : nodes) {
            parameters.put(node.tegName(), node.body());
        }
        //获取附加参数
        Attach attach = Jackson.object(xpath.evalNode("//attach").body(), Attach.class);
        //依据前端传递的支付商户简称查询支付配置信息
        ResponseEntity<PaymentSign> paymentSignResponseEntity = dataServerFeign.findPaymentSignByPaymentName(attach.getPaymentName());
        if(Objects.isNull(paymentSignResponseEntity)||paymentSignResponseEntity.getStatusCode().isError()){
            return ResponseEntity.badRequest().body(Tips.of(-1,"未找到支付配置信息"));
        }
        PaymentSign paymentSign = paymentSignResponseEntity.getBody();
        if(!Objects.equals(paymentSign.getPayPlatformType(), PayPlatformType.WEIXIN.name())){
            return ResponseEntity.badRequest().body(Tips.of(-1,"支付配置信息与调用接口不匹配"));
        }
        PaymentProperties.WeChatPayConfig weChatPayConfig = weChatUtil.getProperties().getWeChatPayConfig();
        weChatPayConfig.setPartnerId(paymentSign.getPartnerId());//设置支付商户
        weChatPayConfig.setPartnerKey(paymentSign.getPartnerKey());
        weChatUtil.getProperties().setWeChatPayConfig(weChatPayConfig);//覆盖原有配置文件中的信息

        //计算签名
        String backSignResult = weChatUtil.createSign(weChatUtil.getProperties().getWeChatPayConfig().getPartnerKey(), parameters);
        log.info("backSignResult:" + backSignResult);
        log.info("urlsign:" + xpath.evalNode("//sign"));

        if ("SUCCESS".equalsIgnoreCase(resultCode) && Objects.equals(backSignResult, xpath.evalNode("//sign").body())) {
            //幂等处理
            PaymentLog paymentLog = paymentLogService.getPaymentLogByPayCodeAndPayStep(payCode, PayStepType.NOTIFY.name());
            if (Objects.nonNull(paymentLog)) {
                return ResponseEntity.ok(Tips.of(1,"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<xml><return_code><![CDATA[SUCCESS]]></return_code>"
                        + "<return_msg><![CDATA[OK]]></return_msg></xml>"));
            }
            int fee = Integer.parseInt(xpath.evalNode("//total_fee").body());
            if(paymentLog.getFee()!=fee){
                log.error("支付金额与回调金额不一致{},{}",fee,paymentLog);
                return ResponseEntity.badRequest().body(Tips.of(-1,"支付金额与回调金额不一致"));
            }

            //获取传达的附加参数获取用户信息
            log.info("attach:" + attach + "-fee:" + fee);
            paymentLog.setPayStep(PayStepType.NOTIFY);//支付步骤
            paymentLog.setPayAt(new Timestamp(DateFormatUtil.convertPayTime(xpath.evalNode("//time_end").body(), "yyyyMMddHHmmss").getTime()));
            paymentLog.setTradeId(xpath.evalNode("//transaction_id").body());
            paymentLog.setBankType(xpath.evalNode("//bank_type").body());

            //记录日志
            paymentLogService.updatePaymentLog(paymentLog);
            //XXXTODO 发送到队列，发送模板消息以及计算提成
            //广播订单支付成功true, "success"
            return ResponseEntity.ok(Tips.of(1,"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<xml><return_code><![CDATA[SUCCESS]]></return_code>"
                    + "<return_msg><![CDATA[OK]]></return_msg></xml>"));
        }
        return ResponseEntity.badRequest().body(Tips.of(-1,"回调信息处理失败"));
    }


    @ApiOperation(value = "微信退款")
    @PutMapping("/refund/{payCode}")
    public ResponseEntity<Tips> refund(@PathVariable("payCode") String payCode,
                                       @RequestParam("appid") String appid,
                                       @RequestParam("paymentName") String paymentName,
                                       @RequestParam("totalFee") int totalFee,
                                       @RequestParam("refundFee") int refundFee,
                                       @RequestParam("refundMemo") String refundMemo) {
        //只退一次 退款单编号就是支付编号

        ResponseEntity<PaymentSign> paymentSignResponseEntity = dataServerFeign.findPaymentSignByPaymentName(paymentName);
        if(Objects.isNull(paymentSignResponseEntity)||paymentSignResponseEntity.getStatusCode().isError()){
            return ResponseEntity.badRequest().body(Tips.of(-1,"未找到支付配置信息"));
        }
        PaymentSign paymentSign = paymentSignResponseEntity.getBody();
        if(!Objects.equals(paymentSign.getPayPlatformType(), PayPlatformType.WEIXIN.name())){
            return ResponseEntity.badRequest().body(Tips.of(-1,"支付配置信息与调用接口不匹配"));
        }
        PaymentProperties.WeChatPayConfig weChatPayConfig = weChatUtil.getProperties().getWeChatPayConfig();
        weChatPayConfig.setPartnerId(paymentSign.getPartnerId());//设置支付商户
        weChatPayConfig.setPartnerKey(paymentSign.getPartnerKey());
        weChatPayConfig.setPkcs12(resourceLoader.getResource(paymentSign.getPkcs12Url()));//网络加载微信退款签名文件
        weChatUtil.getProperties().setWeChatPayConfig(weChatPayConfig);//覆盖原有配置文件中的信息
        boolean refundResult = weChatUtil.refund(appid,payCode,totalFee,refundFee);//退款
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

    @ApiOperation("取消微信支付处理")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "appid", value = "微信应用appid", required = true, dataType = "String"),
            @ApiImplicitParam(paramType = "query", name = "payCode", value = "支付code", required = true, dataType = "String")
    })
    @PutMapping("/cancel")
    public ResponseEntity<Tips> cancel(@RequestParam("appid") String appid,@RequestParam("payCode") String payCode) {
        weChatUtil.cancel(appid,payCode);
        return ResponseEntity.ok(Tips.of(1,"取消微信支付成功"));
    }
}
