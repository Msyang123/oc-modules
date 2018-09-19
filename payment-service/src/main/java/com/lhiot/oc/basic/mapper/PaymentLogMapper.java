package com.lhiot.oc.basic.mapper;

import com.lhiot.oc.basic.domain.PaymentLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface PaymentLogMapper {


    int insertPaymentLog(PaymentLog paymentLog);

    int updatePaymentLog(PaymentLog paymentLog);

    PaymentLog getPaymentLog(Long orderId);

    PaymentLog getPaymentLogByCode(String orderCode);

    List<PaymentLog> getPaymentLogByOrderIdandPayStep(Map<String, Object> param);

}
