package com.lhiot.oc.basic.service;

import com.lhiot.oc.basic.domain.PaymentLog;
import com.lhiot.oc.basic.mapper.PaymentLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@Slf4j
public class PaymentLogService {

    private final PaymentLogMapper paymentLogMapper;


    @Autowired
    public PaymentLogService(PaymentLogMapper paymentLogMapper){
        this.paymentLogMapper=paymentLogMapper;
    }
    /**
     * 依据订单编号查询支付日志
     * @param orderId
     * @return
     */
    public PaymentLog getPaymentLog(Long orderId){
        return paymentLogMapper.getPaymentLog(orderId);
    }

    /**
     * 依据订单编码查询支付日志
     * @param orderCode
     * @return
     */
    public PaymentLog getPaymentLogByCode(String orderCode){
        return paymentLogMapper.getPaymentLogByCode(orderCode);
    }

    /**
     * 添加日志
     * @param paymentLog
     * @return
     */
    public int insertPaymentLog(PaymentLog paymentLog){
        return paymentLogMapper.insertPaymentLog(paymentLog);
    }

    /**
     * 回调修改日志
     * @return
     */
    public int updatePaymentLog(PaymentLog paymentLog){
        return paymentLogMapper.updatePaymentLog(paymentLog);
    }

    /**
     * 依据订单编码查询支付日志
     * @param orderId
     * @param payStep
     * @return
     */
    public List<PaymentLog> getPaymentLogByOrderIdandPayStep(String orderId, String payStep){
        Map<String,Object> param = new HashMap<String,Object>();
        param.put("orderId",orderId);
        param.put("payStep",payStep);
        return paymentLogMapper.getPaymentLogByOrderIdandPayStep(param);
    }
}
