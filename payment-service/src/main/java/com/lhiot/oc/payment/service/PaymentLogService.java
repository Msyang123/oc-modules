package com.lhiot.oc.payment.service;

import com.leon.microx.support.result.Tips;
import com.leon.microx.util.StringUtils;
import com.lhiot.oc.payment.domain.PaymentFlow;
import com.lhiot.oc.payment.domain.PaymentLog;
import com.lhiot.oc.payment.domain.PaymentRefund;
import com.lhiot.oc.payment.domain.SignParam;
import com.lhiot.oc.payment.domain.enums.PayPlatformType;
import com.lhiot.oc.payment.domain.enums.PayStepType;
import com.lhiot.oc.payment.domain.enums.SourceType;
import com.lhiot.oc.payment.feign.BaseUserServerFeign;
import com.lhiot.oc.payment.feign.domain.UserDetailResult;
import com.lhiot.oc.payment.mapper.PaymentFlowMapper;
import com.lhiot.oc.payment.mapper.PaymentLogMapper;
import com.lhiot.oc.payment.mapper.PaymentRefundMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional
@Slf4j
public class PaymentLogService {

    private final PaymentLogMapper paymentLogMapper;
    private final PaymentFlowMapper paymentFlowMapper;
    private final PaymentRefundMapper paymentRefundMapper;
    private final BaseUserServerFeign baseUserServerFeign;


    @Autowired
    public PaymentLogService(PaymentLogMapper paymentLogMapper, PaymentFlowMapper paymentFlowMapper, PaymentRefundMapper paymentRefundMapper, BaseUserServerFeign baseUserServerFeign){
        this.paymentLogMapper=paymentLogMapper;
        this.paymentFlowMapper = paymentFlowMapper;
        this.paymentRefundMapper = paymentRefundMapper;
        this.baseUserServerFeign = baseUserServerFeign;
    }

    /**
     * 依据订单编码查询支付日志
     * @param payCode
     * @return
     */
    public PaymentLog getPaymentLogByPayCode(String payCode){
        return paymentLogMapper.getPaymentLogByPayCode(payCode);
    }

    /**
     * 添加日志
     * @param paymentLog
     * @return
     */
    public int insertPaymentLog(PaymentLog paymentLog){
        //非空就不操作
        if(Objects.isNull(paymentLogMapper.getPaymentLogByPayCode(paymentLog.getPayCode()))){
            return 0;
        }
        int result = paymentLogMapper.insertPaymentLog(paymentLog);
        if(result>0) {
            PaymentFlow paymentFlow = new PaymentFlow();
            paymentFlow.setCreateAt(new Date());
            paymentFlow.setPaymentLogId(paymentLog.getId());
            paymentFlow.setPreStatus(null);
            paymentFlow.setStatus(paymentLog.getPayStep());
            paymentFlowMapper.create(paymentFlow);
        }
        return result;
    }

    /**
     * 添加日志
     * @param signParam
     * @return
     */
    public int insertPaymentLog(SignParam signParam){
        PaymentLog paymentLog = new PaymentLog();
        paymentLog.setPayStep(PayStepType.SIGN);//支付步骤：sign-签名成功
        paymentLog.setBaseUserId(signParam.getAttach().getBaseuserId());
        paymentLog.setUserId(signParam.getAttach().getUserId());
        paymentLog.setApplicationType(signParam.getAttach().getApplicationType());
        paymentLog.setSourceType(signParam.getAttach().getSourceType());
        paymentLog.setPayPlatformType(PayPlatformType.ALIPAY);
        paymentLog.setFee(signParam.getFee());//支付金额
        paymentLog.setPayCode(signParam.getPayCode());
        paymentLog.setSignAt(new Timestamp(System.currentTimeMillis()));
        return this.insertPaymentLog(paymentLog);
    }

    /**
     * 回调修改日志
     * @return
     */
    public int updatePaymentLog(PaymentLog paymentLog,String... refundMemo){

        PaymentLog searchPaymentLong = paymentLogMapper.getPaymentLogByPayCode(paymentLog.getPayCode());
        if(Objects.isNull(searchPaymentLong)){
            return 0;
        }
        paymentLog.setId(searchPaymentLong.getId());
        int result = paymentLogMapper.updatePaymentLog(paymentLog);

        if(result>0) {
            //写支付流水
            Date current = new Date();
            PaymentFlow paymentFlow = new PaymentFlow();
            paymentFlow.setCreateAt(current);
            paymentFlow.setPaymentLogId(paymentLog.getId());
            paymentFlow.setPreStatus(searchPaymentLong.getPayStep());//上一步
            paymentFlow.setStatus(paymentLog.getPayStep());
            paymentFlowMapper.create(paymentFlow);
            //如果是退款就写退款记录
            if(Objects.equals(paymentLog.getPayStep(),PayStepType.REFUND)){
                PaymentRefund paymentRefund=new PaymentRefund();
                paymentRefund.setCreateTime(current);
                paymentRefund.setPaymentLogId(paymentLog.getId());
                paymentRefund.setRefundMemo(refundMemo[0]);//退款理由
                paymentRefundMapper.create(paymentRefund);
            }
        }
        return result;
    }

    /**
     * 依据支付编码查询支付日志
     * @param payCode
     * @param payStep
     * @return
     */
    public PaymentLog getPaymentLogByPayCodeAndPayStep(String payCode, String payStep){
        Map<String,Object> param = new HashMap<String,Object>();
        param.put("payCode",payCode);
        param.put("payStep",payStep);
        return paymentLogMapper.getPaymentLogByPayCodeAndPayStep(param);
    }

    public Tips validateSignParam(SignParam signParam) {
        if(Objects.isNull(signParam)){
            return Tips.of("-1", "支付参数不能为空");
        }
        if (StringUtils.isBlank(signParam.getMemo())) {
            return Tips.of("-1", "支付项目不能为空");
        }
        if (signParam.getFee() <= 0) {
            return Tips.of("-1", "支付金额必须大于0");
        }
        //验证业务信息
        if (Objects.equals(signParam.getAttach().getSourceType(), SourceType.RECHARGE)) {
            if (Objects.isNull(signParam.getAttach().getBaseuserId())) {
                return Tips.of("-1", "充值基础用户信息不能为空");
            }
            ResponseEntity<UserDetailResult> baseUser = baseUserServerFeign.findBaseUserById(signParam.getAttach().getBaseuserId());
            if(Objects.isNull(baseUser)||baseUser.getStatusCode().isError()){
                return Tips.of("-1", "充值基础用户不存在");
            }
        }
        if (StringUtils.isBlank(signParam.getPayCode())) {
            return Tips.of("-1", "支付编码不能为空");
        }
        return Tips.of("1", "验证成功");
    }
}
