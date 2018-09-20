package com.lhiot.oc.basic.mapper;

import com.lhiot.oc.basic.domain.PaymentLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface PaymentLogMapper {


    int insertPaymentLog(PaymentLog paymentLog);

    int updatePaymentLog(PaymentLog paymentLog);


    PaymentLog getPaymentLogByPayCode(String payCode);

    PaymentLog getPaymentLogByPayCodeAndPayStep(Map<String, Object> param);

}
