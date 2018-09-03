package com.lhiot.oc.basic.api;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.leon.microx.common.wrapper.Tips;
import com.leon.microx.util.Exceptions;
import com.leon.microx.util.Jackson;
import com.leon.microx.util.xml.XNode;
import com.leon.microx.util.xml.XPathParser;
import com.lhiot.oc.basic.domain.Attach;
import com.lhiot.oc.basic.domain.PaymentLog;
import com.lhiot.oc.basic.domain.enums.PublishExchange;
import com.lhiot.oc.basic.service.PaymentLogService;
import com.lhiot.oc.basic.service.PaymentService;
import com.lhiot.oc.basic.service.payment.PaymentProperties;
import com.lhiot.oc.basic.service.payment.WeChatUtil;
import com.lhiot.order.domain.BaseOrderInfo;
import com.lhiot.order.domain.enums.OrderStatus;
import com.lhiot.order.domain.enums.PublishExchange;
import com.lhiot.order.domain.payment.Attach;
import com.lhiot.order.domain.payment.PaymentLog;
import com.lhiot.order.domain.payment.SignParam;
import com.lhiot.order.service.BaseOrderService;
import com.lhiot.order.service.payment.*;
import com.lhiot.order.util.DateFormatUtil;
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
    private final BaseOrderService baseOrderService;
    final private RabbitTemplate rabbit;

    @Autowired
    public WxPaymentApi(PaymentService paymentService, PaymentProperties properties,
                        PaymentLogService paymentLogService, BaseOrderService baseOrderService, RabbitTemplate rabbit) {
        this.paymentService = paymentService;
        this.weChatUtil = new WeChatUtil(properties);
        this.paymentLogService = paymentLogService;
        this.baseOrderService = baseOrderService;
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
    public ResponseEntity<Tips> sign(HttpServletRequest request, @RequestBody SignParam signParam) throws Exception {
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
        XPathParser xpath = weChatUtil.getParametersByWeChatCallback(request);
        XPathWrapper wrap = new XPathWrapper(xpath);
        String resultCode = wrap.get("result_code");
        //获取签名的单号
        String orderCode = wrap.get("out_trade_no");
        List<XNode> nodes = xpath.evalNodes("//xml/*");
        SortedMap<Object, Object> parameters = new TreeMap();
        for (XNode node : nodes) {
            parameters.put(node.tegName(), node.body());
        }
        //计算签名
        String signResult = weChatUtil.createSign(weChatUtil.getProperties().getWeChatPay().getLhiot().getPartnerKey(), parameters);
        log.info("signResult:" + signResult);
        log.info("urlsign:" + wrap.get("sign"));

        if ("SUCCESS".equalsIgnoreCase(resultCode) && Objects.equals(signResult, wrap.get("sign"))) {
            //幂等处理
            PaymentLog paymentLog = paymentLogService.getPaymentLogByCode(orderCode);
            if (Objects.nonNull(paymentLog) && Objects.equals(paymentLog.getPayStep(), "paid")) {
                return ResponseEntity.ok("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<xml><return_code><![CDATA[SUCCESS]]></return_code>"
                        + "<return_msg><![CDATA[OK]]></return_msg></xml>");
            }
            int fee = Integer.parseInt(wrap.get("total_fee"));
            if(paymentLog.getPayFee()!=fee){
                log.error("支付金额与回调金额不一致{},{}",fee,paymentLog);
                return ResponseEntity.ok().build();
            }
            Attach attach = Jackson.object(wrap.get("attach"), Attach.class);

            //获取传达的附加参数获取用户信息
            log.info("attach:" + attach + "-fee:" + fee);

            paymentLog.setPayStep("paid");//支付步骤：sign-签名成功 paid-支付成功
            paymentLog.setPayAt(new Timestamp(DateFormatUtil.convertPayTime(wrap.get("time_end"), "yyyyMMddHHmmss").getTime()));
            paymentLog.setTradeId(wrap.get("transaction_id"));
            paymentLog.setBankType(wrap.get("bank_type"));

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


    //退款接口
    @ApiOperation(value = "")
    @GetMapping("/refund")
    public ResponseEntity teambuyRefund(@RequestParam("orderId") Long orderId, @RequestParam(value = "reason", required = false) String reason) {
        //只退一次 退款单编号就是订单编号
        BaseOrderInfo baseOrderInfo = baseOrderService.findOrderById(orderId,true,true,true,true,true);
        if (weChatUtil.refund(baseOrderInfo.getCode(),Integer.valueOf(baseOrderInfo.getAmountPayable()))) {
            BaseOrderInfo updateOrderInfo = new BaseOrderInfo();
            updateOrderInfo.setId(orderId);
            updateOrderInfo.setStatus(OrderStatus.ALREADY_RETURN);
            updateOrderInfo.setReason(reason);
            baseOrderService.update(updateOrderInfo);
            paymentService.recordOrderRefund( baseOrderInfo.getUserId(), orderId, baseOrderInfo.getCode(), reason,baseOrderInfo.getAmountPayable(),"");
            //发布广播消息 add Limiaojun by 20180609
            try {
                rabbit.convertAndSend("order-refund-event", "", Jackson.json(baseOrderInfo));
            } catch (JsonProcessingException e) {
                log.error("退款接口发布广播消息" + e.getLocalizedMessage());
                throw Exceptions.unchecked(e);
            }
            return ResponseEntity.ok("退款成功");
        } else {
            return ResponseEntity.badRequest().body("退款失败");
        }
    }

    @ApiOperation("取消支付")
    @ApiImplicitParam(paramType = "path", name = "orderId", value = "订单ID", required = true, dataType = "Long")
    @PutMapping("/cancel/paying/{orderId}")
    public ResponseEntity cancelPaying(@PathVariable Long orderId) {
        BaseOrderInfo searchBaseOrderInfo = baseOrderService.findOrderById(orderId,true,true,true,true,true);
        //已经不需要支付中状态了
        if (!Objects.equals(OrderStatus.WAIT_PAYMENT, searchBaseOrderInfo.getStatus())) {
            return ResponseEntity.badRequest().body("订单状态不正确");
        }
        BaseOrderInfo baseOrderInfo = new BaseOrderInfo();
        baseOrderInfo.setId(orderId);
        baseOrderInfo.setStatus(OrderStatus.FAILURE);
        baseOrderService.updateOrderStatusById(baseOrderInfo);
        try {
            rabbit.convertAndSend(PublishExchange.REFUND_EXCHANGE.getName(), "", Jackson.json(baseOrderInfo));
        } catch (JsonProcessingException e) {
            log.error("取消订单发布广播消息" + e.getLocalizedMessage());
            throw Exceptions.unchecked(e);
        }
        return ResponseEntity.ok().build();
    }
}
