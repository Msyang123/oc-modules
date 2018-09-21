package com.lhiot.oc.payment.mapper;

import com.lhiot.oc.payment.domain.PaymentRefund;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
* Description:支付退款记录Mapper类
* @author yijun
* @date 2018/09/20
*/
@Mapper
public interface PaymentRefundMapper {

    /**
    * Description:新增支付退款记录
    *
    * @param paymentRefund
    * @return
    * @author yijun
    * @date 2018/09/20 11:49:08
    */
    int create(PaymentRefund paymentRefund);

    /**
    * Description:根据id修改支付退款记录
    *
    * @param paymentRefund
    * @return
    * @author yijun
    * @date 2018/09/20 11:49:08
    */
    int updateById(PaymentRefund paymentRefund);

    /**
    * Description:根据ids删除支付退款记录
    *
    * @param ids
    * @return
    * @author yijun
    * @date 2018/09/20 11:49:08
    */
    int deleteByIds(java.util.List<String> ids);

    /**
    * Description:根据id查找支付退款记录
    *
    * @param id
    * @return
    * @author yijun
    * @date 2018/09/20 11:49:08
    */
    PaymentRefund selectById(Long id);

    /**
    * Description:查询支付退款记录列表
    *
    * @param paymentRefund
    * @return
    * @author yijun
    * @date 2018/09/20 11:49:08
    */
     List<PaymentRefund> pagePaymentRefunds(PaymentRefund paymentRefund);


    /**
    * Description: 查询支付退款记录总记录数
    *
    * @param paymentRefund
    * @return
    * @author yijun
    * @date 2018/09/20 11:49:08
    */
    long pagePaymentRefundCounts(PaymentRefund paymentRefund);
}
