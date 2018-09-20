package com.lhiot.oc.basic.service.payment;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConstants;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeAppPayModel;
import com.alipay.api.domain.AlipayTradeCancelModel;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.request.AlipayTradeCancelRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.alipay.api.response.AlipayTradeCancelResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.leon.microx.support.result.Tips;
import com.leon.microx.util.Jackson;
import com.leon.microx.util.StringUtils;
import com.lhiot.oc.basic.domain.SignParam;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AliPayUtil {

    @Getter
    private PaymentProperties properties;
    @Getter
    private AlipayClient alipayClient;
    @Getter
    private long orderTimeoutMs;

    public AliPayUtil(PaymentProperties properties) {
        this.properties = properties;
        this.orderTimeoutMs = properties.getTemporaryOrderExpirationMs();
    }

    public Tips sign(AlipayTradeAppPayModel model) throws AlipayApiException {

        AlipayTradeAppPayRequest request = new AlipayTradeAppPayRequest();
        request.setBizModel(model);
        request.setNotifyUrl(properties.getAliPayConfig().getNotifyUrl());
        initAlipayClientWithProp();
        AlipayTradeAppPayResponse signed=alipayClient.sdkExecute(request);
        if (Objects.nonNull(signed) && Objects.nonNull(signed.getBody())) {
            return Tips.of("1",signed.getBody());
        }
        return Tips.of("-1","支付宝签名失败");
    }

    /**
     * 支付宝撤销支付
     * @param model
     * @return
     * @throws AlipayApiException
     */
    public Tips cancel(AlipayTradeCancelModel model) throws AlipayApiException {
        AlipayTradeCancelRequest request=new AlipayTradeCancelRequest();
        request.setBizModel(model);
        request.setNotifyUrl(properties.getAliPayConfig().getCancelNotifyUrl());

        initAlipayClientWithProp();
        AlipayTradeCancelResponse signed=alipayClient.sdkExecute(request);
        if (Objects.nonNull(signed) && Objects.nonNull(signed.getBody())) {
            return Tips.of("1",signed.getBody());
        }
        return Tips.of("-1","支付宝撤销失败");
    }

    /**
     * 支付宝退款
     *
     * @param payCode 商户订单号
     * @param refundFee 退款总额(元)
     * @param reason    退款原因
     * @param refundId  退款单号（部分退款必填，如果部分退款只退一次，可以是订单号）
     * @return true or false
     * @throws Exception HTTP Request Exception
     */
    public boolean refund(String payCode, String refundFee, String reason, String refundId) throws Exception {
        AlipayTradeRefundModel model = new AlipayTradeRefundModel();
        model.setOutTradeNo(payCode); // 支付时传入的商户订单号，与trade_no必填一个
        model.setRefundAmount(refundFee);
        if (StringUtils.isNotBlank(reason)) {
            model.setRefundReason(reason);
        }
        if (StringUtils.isNotBlank(refundId)) {
            model.setOutRequestNo(refundId);
        }

        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        request.setBizModel(model);
        initAlipayClientWithProp();
        AlipayTradeRefundResponse response = alipayClient.execute(request);
        return response.isSuccess();
    }

    public AlipayTradeAppPayModel createAliPayTradeAppPayModel(SignParam param){
        AlipayTradeAppPayModel model = new AlipayTradeAppPayModel();
        model.setSellerId(properties.getAliPayConfig().getSellerId());
        model.setBody(param.getMemo());
        model.setSubject(param.getMemo());
        model.setPassbackParams(Jackson.json(param.getAttach()));//设置附加参数
        model.setOutTradeNo(param.getPayCode());
        model.setTimeoutExpress(properties.getAliPayConfig().getTimeoutExpress());//设置支付宝支付6分钟超时
        model.setProductCode("QUICK_MSECURITY_PAY");
        return model;
    }

    /**
     * 从支付宝回调请求中获取参数
     */
    public Map<String, String> aliPayNotifyParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((name, values) -> {
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                if (i == values.length - 1) {
                    valueStr = valueStr + values[i];
                } else {
                    valueStr = valueStr + values[i] + ",";
                }
            }
            params.put(name, valueStr);
        });
        return params;
    }


    /**
     * 校验通知中的seller_id、app_id是否为out_trade_no这笔单据的对应的操作方（有的时候，一个商户可能有多个seller_id/seller_email）
     */
    public boolean verifySeller(Map<String, String> data) {
        boolean signVerified = false;
        try {
            signVerified = AlipaySignature.rsaCheckV1(data, properties.getAliPayConfig().getAliPayPublicKey(), properties.getCharset(), properties.getAliPayConfig().getSignType());
        } catch (AlipayApiException e) {
            log.error(e.getMessage(), e);
        }
        if (!signVerified) {
            log.error("*****************支付宝回调 === 签名验证错误. 返回failure.****************");
            return false;
        }

        if (!Objects.equals(properties.getAliPayConfig().getSellerId(), data.get("seller_id"))) {
            log.error("*****************支付宝回调 === seller_id 检查失败. 返回failure.****************");
            return false;
        }

        if (!Objects.equals(properties.getAliPayConfig().getAppId(), data.get("app_id"))) {
            log.error("*****************支付宝回调 === app_id 检查失败. 返回failure.****************");
            return false;
        }
        return true;
    }

    /**
     * 分转元
     */
    public static String fenToYuan(Long fen) {
        return BigDecimal.valueOf(fen).divide(new BigDecimal(100), 2, RoundingMode.UP).toString();
    }

    /**
     * 元转分
     */
    public static String yuanToFen(String yuan) {
        return new BigDecimal(yuan).multiply(new BigDecimal(100)).setScale(0, RoundingMode.DOWN).toString();
    }

    /**
     * 依据配置初始化支付宝客户端工具类
     * 每次配置都是远程获取配置加载
     */
    private void initAlipayClientWithProp(){
        this.alipayClient = new DefaultAlipayClient(
                properties.getAliPayConfig().getApiUrl(),
                properties.getAliPayConfig().getAppId(),
                properties.getAliPayConfig().getAliPayPrivateKey(),
                AlipayConstants.FORMAT_JSON,
                properties.getCharset(),
                properties.getAliPayConfig().getAliPayPublicKey(),
                properties.getAliPayConfig().getSignType()
        );
    }

    /**
     * 从支付宝回调请求中获取参数
     */
    /*public NotifyParam aliPayNotifyParams(Map<String, String> params) throws IOException {
        NotifyParam nvp = new NotifyParam();
        nvp.setTradeId(params.get("trade_no"));
        Date payTime = DateFormatUtil.format1(params.get("gmt_payment"));
        nvp.setPayTime(payTime);
        nvp.setBankType(params.get("fund_bill_list"));
        nvp.setOrderCode(params.get("out_trade_no"));
        String yuan = yuanToFen(params.get("total_amount"));
        nvp.setPayFee(Integer.valueOf(yuan));
        if (params.containsKey("passback_params")) {
            Attach attach = Jackson.object(params.get("passback_params"), Attach.class);
            nvp.setPayUserId(attach.getUserId());
            nvp.setSourceType(PaymentTools.convertPaySourceType(attach.get("sourceType") + StringUtils.NULL_VALUE));
            nvp.setPayType(PaymentTools.convertPayType(attach.get("payType") + StringUtils.NULL_VALUE));
        }
        return nvp;
    }*/
}
