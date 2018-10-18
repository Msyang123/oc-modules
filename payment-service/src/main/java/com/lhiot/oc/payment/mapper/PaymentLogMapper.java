package com.lhiot.oc.payment.mapper;

import com.lhiot.oc.payment.domain.PaymentLog;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Mapper
@Repository
public interface PaymentLogMapper {


    int insertPaymentLog(PaymentLog paymentLog);

    int updatePaymentLog(PaymentLog paymentLog);


    PaymentLog getPaymentLogByPayCode(String payCode);

    PaymentLog getPaymentLogByPayCodeAndPayStep(Map<String, Object> param);

}
