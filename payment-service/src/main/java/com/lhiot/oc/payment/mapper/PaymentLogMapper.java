package com.lhiot.oc.payment.mapper;

import com.lhiot.oc.payment.domain.PaymentLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.Map;

@Mapper
public interface PaymentLogMapper {


    int insertPaymentLog(PaymentLog paymentLog);

    int updatePaymentLog(PaymentLog paymentLog);


    PaymentLog getPaymentLogByPayCode(String payCode);

    PaymentLog getPaymentLogByPayCodeAndPayStep(Map<String, Object> param);

}
