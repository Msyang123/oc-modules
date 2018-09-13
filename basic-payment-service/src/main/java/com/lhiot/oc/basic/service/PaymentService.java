package com.lhiot.oc.basic.service;


import com.alipay.api.AlipayClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.leon.microx.support.result.Tips;
import com.leon.microx.util.Jackson;
import com.leon.microx.util.SnowflakeId;
import com.leon.microx.util.StringUtils;
import com.lhiot.oc.basic.domain.Attach;
import com.lhiot.oc.basic.domain.PaymentLog;
import com.lhiot.oc.basic.domain.SignParam;
import com.lhiot.oc.basic.domain.enums.NormalExchange;
import com.lhiot.oc.basic.domain.enums.OrderStatus;
import com.lhiot.oc.basic.domain.enums.OrderType;
import com.lhiot.oc.basic.domain.enums.PublishExchange;
import com.lhiot.oc.basic.feign.BaseUserServerFeign;
import com.lhiot.oc.basic.service.payment.AliPayUtil;
import com.lhiot.oc.basic.service.payment.PaymentProperties;
import com.lhiot.oc.basic.service.payment.WeChatUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;


@Slf4j
@Service
public class PaymentService {

    private final static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final RabbitTemplate rabbit;

    private static final ZoneId LOCAL_ZONE_ID = ZoneId.of("Asia/Shanghai");

    private final PaymentLogService paymentLogService;
    private final BaseOrderService baseOrderService;
    private final BaseUserServerFeign baseUserServerFeign;
    final private ThirdPartyServiceFeign thirdPartyServiceFeign;
    private final OrderRefundMapper orderRefundMapper;

    private final SnowflakeId snowflakeId;

    private AliPayUtil aliPayUtil;
    private WeChatUtil weChatUtil;

    private static final String HD_CANCEL_ORDER_SUCCESS_RESULT_STRING = "{\"success\":true}";

    @Autowired
    public PaymentService(RabbitTemplate rabbit, PaymentProperties properties, AlipayClient alipayClient, PaymentLogService paymentLogService,
                          BaseOrderService baseOrderService, BaseUserServerFeign baseUserServerFeign,
                          ThirdPartyServiceFeign thirdPartyServiceFeign, OrderRefundMapper orderRefundMapper, SnowflakeId snowflakeId) {
        this.rabbit = rabbit;
        this.paymentLogService = paymentLogService;
        this.baseOrderService = baseOrderService;
        this.baseUserServerFeign = baseUserServerFeign;
        this.thirdPartyServiceFeign = thirdPartyServiceFeign;
        this.orderRefundMapper = orderRefundMapper;
        this.snowflakeId = snowflakeId;
        this.aliPayUtil = new AliPayUtil(properties, alipayClient);
        this.weChatUtil = new WeChatUtil(properties);

    }



    /**
     * 微信退款
     *
     * @param orderCode 商户订单号
     * @param totalFee  必须是订单的支付金额，单位：分
     * @param refundFee 少于订单支付金额，单位：分
     * @param refundId  (out_refund_no)  退款单号（部分退款必填 可以不同，如果部分退款只退一次，可以是订单号）
     * @return true or false
     * @throws Exception HTTP Request Exception
     */
    /*public OrderWxRefundResult refund(Long orderId, String orderCode, int totalFee, int refundFee, String refundId, WeChatUtil weChatUtil) throws Exception {
        PaymentProperties.WeChatPayConfig config = weChatUtil.getProperties().getWeChatPay();
        LocalDateTime start = LocalDateTime.now(LOCAL_ZONE_ID);
        String currTime = DATE_TIME_FORMATTER.format(start);
        String nonceStr = currTime.substring(8, currTime.length()) + Random.randomInteger(4);
        SortedMap<Object, Object> packageParams = new TreeMap<>();
        packageParams.put("appid", weChatUtil.getProperties().getWeChatOauth().getAppId());
        packageParams.put("mch_id", config.getLhiot().getPartnerId());
        packageParams.put("nonce_str", nonceStr);
        packageParams.put("out_trade_no", orderCode);//TODO 此处orderCode 应为订单Id与签名保持一致 待讨论
        packageParams.put("out_refund_no", orderCode);
        packageParams.put("total_fee", totalFee); // 订单总额
        packageParams.put("refund_fee", refundFee); // 退款金额
        packageParams.put("op_user_id", config.getLhiot().getPartnerId());
        packageParams.put("sign", weChatUtil.createSign(config.getLhiot().getPartnerKey(), packageParams));

        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        InputStream in = null;
        try {
            in = config.getLhiot().getPkcs12().getInputStream();
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            keystore.load(in, config.getLhiot().getPartnerId().toCharArray());
            SSLContext sslContext = SSLContexts.custom().loadKeyMaterial(keystore, config.getLhiot().getPartnerId().toCharArray()).build();
            SSLConnectionSocketFactory sslConnection = new SSLConnectionSocketFactory(sslContext,
                    new String[]{"TLSv1", "TLSv1.1", "TLSv1.2"}, null,
                    SSLConnectionSocketFactory.getDefaultHostnameVerifier()
            );
            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(weChatUtil.getProperties().getHttpConnectionTimeoutExpress()).build();
            httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).setSSLSocketFactory(sslConnection).build();
            HttpPost httpPost = new HttpPost(weChatUtil.REFUND_URL);
            httpPost.setEntity(new StringEntity(weChatUtil.getRequestXml(packageParams), weChatUtil.getProperties().getCharset()));
            response = httpClient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                StatusLine statusLine = response.getStatusLine();
                throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
            }
            String resource = EntityUtils.toString(response.getEntity(), weChatUtil.getProperties().getCharset());
            String resultXml = resource.replace("<![CDATA[", "").replace("]]>", "");
            log.debug("=====微信退款结果=====>>> " + resultXml);
            XPathParser xPath = new XPathParser(resultXml);
            XNode returnCode = xPath.evalNode("//return_code");
            if (Objects.isNull(returnCode)) {
            	log.debug("=====微信退款结果returnCode=====>>> 1" + returnCode);
                throw new RuntimeException(resultXml);
            }
            if (!"SUCCESS".equalsIgnoreCase(returnCode.body())) {
            	log.debug("=====微信退款结果returnCode=====>>> 2" + returnCode.body());
                throw new RuntimeException(xPath.evalNode("//return_msg").body());
            }
            XNode resultCode = xPath.evalNode("//result_code");
            if (!"SUCCESS".equalsIgnoreCase(resultCode.body())) {
            	log.debug("=====微信退款结果returnCode=====>>> 3" + returnCode.body());
                throw new RuntimeException(xPath.evalNode("//err_code_des").body());
            }
            XNode transactionId = xPath.evalNode("//transaction_id");
            OrderWxRefundResult orderWxRefundResult = new OrderWxRefundResult();
            orderWxRefundResult.setRefundResultFlag(true);
            orderWxRefundResult.setTransactionNo(transactionId.body());
            orderWxRefundResult.setOrderId(orderId + "");
            orderWxRefundResult.setRefundFee(refundFee + "");
            orderWxRefundResult.setOrderCode(orderCode);
            log.debug("=====微信退款结果returnCode=====>>> 结束了");
            return orderWxRefundResult;
        } finally {
            IOUtils.closeQuietly(in, response, httpClient);
        }
    }*/

    /**
     * 查询支付日志
     * @param reason
     * @param userId
     * @param orderCode
     * @param orderWxRefundResult
     */
/*    public void recordOrderRefund(String reason, Long userId, Long orderId, String orderCode, OrderWxRefundResult orderWxRefundResult) {
        //增加退款日志
        this.recordOrderRefund(userId,orderId,orderCode,reason,orderWxRefundResult.getRefundFee(),orderWxRefundResult.getTransactionNo());
    }*/
    /**
     * 查询支付日志
     * @param reason
     * @param userId
     * @param orderCode
     */
    public void recordOrderRefund(Long userId, Long orderId, String orderCode,String reason,int refundFee,String transactionNo) {
        //增加退款日志
        OrderRefund orderRefundRecord = new OrderRefund();
        orderRefundRecord.setUserId(userId);
        orderRefundRecord.setOrderId(orderId);
        orderRefundRecord.setOrderCode(orderCode);
        orderRefundRecord.setReason(reason);
        orderRefundRecord.setRefundAt(new Timestamp(System.currentTimeMillis()));
        orderRefundRecord.setRefundFee(""+refundFee);
        orderRefundRecord.setTransactionNo(transactionNo);
        orderRefundMapper.create(orderRefundRecord);
    }

/*    public void sendTempalteMessage(String activityName, int moeny, Long userId) {
        KeywordValue keywordValue = new KeywordValue();
        keywordValue.setKeyword1Value(activityName);
        keywordValue.setKeyword2Value(String.valueOf(moeny / 100.0) + "元");
        keywordValue.setKeyword3Value(DateFormatUtil.format1(new Date()));
        keywordValue.setTemplateType("PAYMENT_SUCCESS");
        keywordValue.setUserId(userId);
        try {
            rabbit.convertAndSend("template-exchange", "send-template-message", Jackson.json(keywordValue));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }*/

    public Tips validateSignParam(SignParam signParam) {
        if (StringUtils.isBlank(signParam.getMemo())) {
            return Tips.of("-1", "支付项目不能为空");
        }
        //签名业务编码
        String code = null;
        int needPay = 0;
        //验证业务信息
        if (Objects.equals(signParam.getAttach().getSourceType(), Attach.SourceType.RECHARGE)) {
            //充值
            if (signParam.getFee() < 1) {
                return Tips.of("-1", "充值金额必须大于0");
            }
            if (Objects.isNull(signParam.getAttach().getBaseuserId())) {
                return Tips.of("-1", "充值基础用户信息不能为空");
            }
            code = snowflakeId.stringId();
            needPay = signParam.getFee();
        } else {
            //订单支付
            if (com.leon.microx.util.StringUtils.isBlank(signParam.getOrderCode())) {
                return Tips.of("-1", "订单编码不能为空");
            }
            BaseOrderInfo baseOrderInfo = baseOrderService.findOrderByCode(signParam.getOrderCode());
            if (Objects.isNull(baseOrderInfo)) {
                return Tips.of("-1", "未找到订单");
            }
            //非待支付状态订单
            if (!Objects.equals(baseOrderInfo.getStatus(), OrderStatus.WAIT_PAYMENT)) {
                return Tips.of("-1", "订单非待支付状态");
            }
            code = signParam.getOrderCode();
            //查询订单支付金额 (应付金额+配送费)
            needPay = baseOrderInfo.getAmountPayable() + baseOrderInfo.getDeliveryAmount();
        }
        signParam.setOrderCode(code);
        signParam.setFee(needPay);
        return Tips.of("1", "验证成功");
    }


    /**
     * 签名后处理逻辑 包括写日志和广播消息
     *
     * @param signParam
     * @throws JsonProcessingException
     */
    public void signSavePaymentLog(SignParam signParam){
        //写签名日志
        PaymentLog paymentLog = null;
        Long orderId = null;
        if (Objects.equals(signParam.getAttach().getSourceType(), Attach.SourceType.ORDER)) {
            //不再需要订单状态改成支付中
            paymentLog = paymentLogService.getPaymentLogByCode(signParam.getOrderCode());

        }
        if (null == paymentLog) {
            paymentLog = new PaymentLog();
            paymentLog.setPayStep("sign");//支付步骤：sign-签名成功 paid-支付成功
            paymentLog.setUserId(signParam.getAttach().getUserId());
            paymentLog.setApplicationTypeEnum(signParam.getAttach().getApplicationTypeEnum());
            paymentLog.setSourceType(signParam.getAttach().getSourceType().toString());
            paymentLog.setPayType(signParam.getPayPlatformType().toString());
            paymentLog.setPayFee(signParam.getFee());//支付金额
            paymentLog.setOrderId(orderId);
            paymentLog.setSignAt(new Timestamp(System.currentTimeMillis()));
            paymentLog.setOrderCode(signParam.getOrderCode());
            paymentLogService.insertPaymentLog(paymentLog);
        } else {
            paymentLog.setPayType(signParam.getPayPlatformType().toString());
            paymentLog.setSignAt(new Timestamp(System.currentTimeMillis()));
            paymentLog.setOrderCode(signParam.getOrderCode());//充值每次都会变化code
            paymentLogService.updatePaymentLog(paymentLog);
        }
        //发布广播消息 后续会改成活动锚点
        rabbit.convertAndSend(PublishExchange.SIGN_EXCHANGE.getName(), "", Jackson.json(paymentLog));
        //发送延时队列
        /*rabbit.convertAndSend(DelayExchange.PAYMENT_DELAY.getExchangeName(), DelayExchange.DelayQueues.PAYMENT_TIEMOUT.getDelayName(), Jackson.json(paymentLog), message -> {
            message.getMessageProperties().setExpiration((6 * 60 * 1000) + "");
            return message;
        });*/
    }

    /**
     * 支付回调处理业务 需要考虑幂等性
     * @param attach     自定义参数
     * @param paymentLog 日志
     */
    public void notifyUpdate(Attach attach, PaymentLog paymentLog){
        //广播支付消息
        rabbit.convertAndSend(PublishExchange.PUBLISH_PAYMENT_NOTIFIED.getName(), "", Jackson.json(paymentLog));
        log.debug("广播支付回调");
        if (Objects.equals(attach.getSourceType(), Attach.SourceType.RECHARGE)) {
            //充值
            BaseUser baseUser = new BaseUser();
            baseUser.setCurrency(paymentLog.getPayFee());
            baseUser.setId(attach.getBaseuserId());
            baseUser.setApplicationType(attach.getApplicationTypeEnum());

            if (this.updateBalance(attach.getApplicationTypeEnum().getDescription() + "-充值",baseUser)) {
                log.error("充值支付回调调用远程充值鲜果币失败:{}", attach);
                return;
            }
        } else {
            //订单支付
            BaseOrderInfo baseOrderInfo = baseOrderService.findOrderById(paymentLog.getOrderId());
            if(Objects.isNull(baseOrderInfo)){
                log.error("订单支付回调失败:未找到订单{}", attach);
                return;
            }
            //因为存在30分钟订单超时失效情况，但是第三方支付可能会再三十分钟后发起回调支付成功，此处应该认为是支付成功
            if(!Objects.equals(baseOrderInfo.getStatus(),OrderStatus.FAILURE)&&
                    !Objects.equals(baseOrderInfo.getStatus(),OrderStatus.WAIT_PAYMENT)){
                log.error("订单支付回调失败:订单状态异常{},{}", attach,baseOrderInfo);
                return;
            }

            BaseOrderInfo updateBaseOrderInfo = new BaseOrderInfo();
            updateBaseOrderInfo.setCode(paymentLog.getOrderCode());
            updateBaseOrderInfo.setStatus(OrderStatus.WAIT_SEND_OUT);//待发货
            baseOrderService.updateOrderStatusByCode(updateBaseOrderInfo);
            //发送海鼎
            //团购订单不在此处发送海鼎 单独调接口发送
            if(Objects.equals(baseOrderInfo.getOrderType(), OrderType.TEAM_BUYING)){
                rabbit.convertAndSend(PublishExchange.PUBLISH_PAYMENT_NOTIFIED.getName(), "", Jackson.json(paymentLog));
                log.debug("广播支付回调");
                return;
            }
            //购买到仓库
            if(Objects.equals(baseOrderInfo.getOrderType(), OrderType.TO_STORE)){
                //TODO 发送到个人仓库 不需要发送海鼎
                //baseUserServerFeign.xxxxx;
                rabbit.convertAndSend(PublishExchange.PUBLISH_PAYMENT_NOTIFIED.getName(), "", Jackson.json(paymentLog));
                log.debug("广播支付回调");
                return;
            }else {
                //通过主题队列异步处理海鼎订单发送与配送业务
                rabbit.convertAndSend(NormalExchange.SEND_TO_HD.getExchangeName(), NormalExchange.SEND_TO_HD.getQueueName(), Jackson.json(baseOrderInfo));
            }
        }
    }

    /**
     * feign 修改用户余额
     *
     * @param memo
     * @param baseUser
     * @return
     */
    public boolean updateBalance(String memo, BaseUser baseUser) {
        ResponseEntity<Object> entity = baseUserServerFeign.updateCurrencyById(baseUser.getId(), memo, baseUser);
        if (Objects.isNull(entity) || entity.getStatusCodeValue() >= 400) {
            return false;
        }
        return true;
    }

    /**
     * 订单退款(包括 取消订单 待收货退款 已收货退款)
     * @param returnOrderParam 退款订单信息
     * @return
     */
    public Tips  refundOrder(ReturnOrderParam returnOrderParam) throws Exception{
        Long orderId=returnOrderParam.getOrderId();
        String reason=returnOrderParam.getReturnReason();

        BaseOrderInfo searchBaseOrderInfo = baseOrderService.findOrderById(orderId);


        if(Objects.isNull(searchBaseOrderInfo)){
            return Tips.of(-1,"未找到订单");
        }
        if (!Objects.equals(OrderStatus.WAIT_PAYMENT, searchBaseOrderInfo.getStatus())
                &&!Objects.equals(OrderStatus.WAIT_SEND_OUT, searchBaseOrderInfo.getStatus())
                &&!Objects.equals(OrderStatus.RECEIVED, searchBaseOrderInfo.getStatus())
                &&!Objects.equals(OrderStatus.RETURNING, searchBaseOrderInfo.getStatus())) {
            return Tips.of(-1,"订单状态不正确");
        }
        //取消订单 不需要发送海鼎信息
        if(Objects.equals(OrderStatus.WAIT_PAYMENT, searchBaseOrderInfo.getStatus())) {
            BaseOrderInfo baseOrderInfo = new BaseOrderInfo();
            baseOrderInfo.setId(orderId);
            baseOrderInfo.setStatus(OrderStatus.FAILURE);
            baseOrderService.updateOrderStatusById(baseOrderInfo);
            rabbit.convertAndSend(PublishExchange.REFUND_EXCHANGE.getName(), "", Jackson.json(baseOrderInfo));
            return Tips.of(1, "取消订单成功");
        }else if(Objects.equals(OrderStatus.WAIT_SEND_OUT, searchBaseOrderInfo.getStatus())
                ||Objects.equals(OrderStatus.RETURNING, searchBaseOrderInfo.getStatus())){
            //待收货 退所有支付金额
            //退货退款中 只退申请退款部分

            // 使用海鼎订单编号 查询海鼎订单状态 防止系统两边订单状态不一致
            ResponseEntity<Map<String, Object>> entity = thirdPartyServiceFeign.hdOrderDetail(searchBaseOrderInfo.getHdOrderCode());
            if (Objects.isNull(entity) || entity.getStatusCodeValue() >= 400) {
                return Tips.of(-1, "查询门店订单信息失败");
            }
            Map<String, Object> hdOrderDetail = entity.getBody();
            if(Objects.isNull(hdOrderDetail)){
                return Tips.of(-1, "未找到门店订单");
            }
            log.info(hdOrderDetail.toString());
            // 海鼎已确认 未备货

            //TODO 需要修改订单商品为退货状态 refund_status
            if (Objects.equals(OrderStatus.WAIT_SEND_OUT, searchBaseOrderInfo.getStatus())
                    && !CollectionUtils.isEmpty(hdOrderDetail)
                    && "confirmed".equals(hdOrderDetail.get("state"))
                    ) {
                //取消订单
                ResponseEntity<String> responseEntity = thirdPartyServiceFeign.hdOrderCancel(searchBaseOrderInfo.getHdOrderCode(),
                        StringUtils.replaceEmoji(returnOrderParam.getReturnReason(), ""));
                String result = responseEntity.getBody();
                if (!Objects.equals(HD_CANCEL_ORDER_SUCCESS_RESULT_STRING, result)) {
                    return Tips.of(-1, "门店退货失败");
                }
            } else if (OrderStatus.RECEIVED.equals(searchBaseOrderInfo.getStatus())
                    && !CollectionUtils.isEmpty(hdOrderDetail)
                    && ("delivering".equals(hdOrderDetail.get("state"))
                    || "delivered".equals(hdOrderDetail.get("state")))) {
                // 海鼎配送中和配送完成
                ResponseEntity<String> responseEntity = thirdPartyServiceFeign.hdOrderRefund(searchBaseOrderInfo);
                if (responseEntity == null || responseEntity.getStatusCodeValue() >= 400) {
                    return  Tips.of(-1, "门店退货失败");
                }
            } else {
                return Tips.of(-1, "当前状态不能退货");
            }

            PaymentLog paymentLog= paymentLogService.getPaymentLogByCode(searchBaseOrderInfo.getCode());
            if(Objects.isNull(paymentLog)){
                return Tips.of(-1,"订单未找到支付记录");
            }
            if(!Objects.equals(paymentLog.getPayStep(),"paid")){
                return Tips.of(-1,"订单未支付");
            }
            //退款金额
            int refundFee=0;
            //订单前一状态
            if(Objects.equals(OrderStatus.RETURNING, searchBaseOrderInfo.getStatus())){
                List<OrderProduct> orderProductList=baseOrderService.findRefundProductsByOrderId(searchBaseOrderInfo.getId());
                if(Objects.isNull(orderProductList)||orderProductList.isEmpty()){
                    return Tips.of(-1,"未找到退货商品");
                }
                //计算退款商品总价
                for(OrderProduct orderProduct:orderProductList){
                    refundFee+=orderProduct.getPrice();
                }
            }else{
                refundFee=paymentLog.getPayFee();
            }
            BaseOrderInfo baseOrderInfo = new BaseOrderInfo();
            baseOrderInfo.setId(orderId);
            if(Objects.equals(SignParam.PayPlatformType.WEIXIN.toString(),paymentLog.getPayType())) {

                boolean weixinRefundResult = weChatUtil.refund(paymentLog.getOrderCode(), paymentLog.getPayFee(),refundFee);
                if (weixinRefundResult) {
                    baseOrderInfo.setStatus(OrderStatus.ALREADY_RETURN);
                    baseOrderService.updateOrderStatusById(baseOrderInfo);
                    //订单退款记录
                    this.recordOrderRefund(baseOrderInfo.getUserId(), orderId, baseOrderInfo.getCode(), reason, refundFee, paymentLog.getTradeId());
                    rabbit.convertAndSend("order-refund-event", "", Jackson.json(baseOrderInfo));
                    return Tips.of(1, "订单微信退款成功");
                } else {
                    return Tips.of(-1, "订单微信退款失败");
                }
            }else if(Objects.equals(SignParam.PayPlatformType.ALIPAY.toString(),paymentLog.getPayType())){
                    boolean alipayRefundResult=aliPayUtil.refund(paymentLog.getOrderCode(), AliPayUtil.fenToYuan(refundFee),reason, paymentLog.getOrderCode());
                    if(alipayRefundResult){
                        baseOrderInfo.setStatus(OrderStatus.ALREADY_RETURN);
                        baseOrderService.updateOrderStatusById(baseOrderInfo);
                        //订单退款记录
                        this.recordOrderRefund(baseOrderInfo.getUserId(), orderId, baseOrderInfo.getCode(), reason, refundFee, paymentLog.getTradeId());
                        rabbit.convertAndSend("order-refund-event", "", Jackson.json(baseOrderInfo));
                        return Tips.of(1,"订单支付宝退款成功");
                    }else{
                        return Tips.of(-1,"订单支付宝退款失败");
                    }
            }else if(Objects.equals(SignParam.PayPlatformType.BALANCE.toString(),paymentLog.getPayType())){
                    BaseUser baseUser=new BaseUser();
                    baseUser.setCurrency(refundFee);
                    baseUser.setId(paymentLog.getUserId());
                    baseUser.setApplicationType(paymentLog.getApplicationTypeEnum());
                    if(this.updateBalance("订单退款:"+reason,baseUser)) {

                        baseOrderInfo.setStatus(OrderStatus.ALREADY_RETURN);
                        baseOrderService.updateOrderStatusById(baseOrderInfo);
                        //订单退款记录
                        this.recordOrderRefund(baseOrderInfo.getUserId(), orderId, baseOrderInfo.getCode(), reason, refundFee, paymentLog.getTradeId());
                        rabbit.convertAndSend("order-refund-event", "", Jackson.json(baseOrderInfo));
                        return Tips.of(1,"订单鲜果币退款成功");
                    }else{
                        return Tips.of(-1,"订单鲜果币退款失败");
                    }
            }else{
                return Tips.of(-1,"未找到匹配的订单支付类型"+paymentLog.getPayType());
            }
        }
        return Tips.of(-1,"订单退款失败");
    }


    /**
     * 发送至队列处理业务
     *
     * @param baseOrderInfo
     * @throws JsonProcessingException
     * @throws Exception
     */
    /*  public void sendToQueue(BaseOrderInfo baseOrderInfo) throws Exception {
        //如果是鲜果师商城的订单
        ApplicationTypeEnum applyType = baseOrderInfo.getApplicationTypeEnum();
        //如果发送海鼎减库存失败，则发送到队列中进行重试
        if (!baseOrderService.sendToHdReduce(baseOrderInfo)) {
            rabbit.convertAndSend(NormalExchange.SEND_TO_HD.getExchangeName(), NormalExchange.SEND_TO_HD.getQueueName(), Jackson.json(baseOrderInfo));
        }
        //如果订单为鲜果师商城的订单，则发送广播，消费端写在鲜果师用户
      if (ApplicationTypeEnum.FRUIT_DOCTOR.equals(applyType)) {
        	baseOrderInfo.setOrderStatus(OrderStatus.WAIT_SEND_OUT);
            rabbit.convertAndSend(PublishExchange.FRUITDOCOTR_PAYMENT.getName(), "", Jackson.json(baseOrderInfo));
        }
    }*/
}
