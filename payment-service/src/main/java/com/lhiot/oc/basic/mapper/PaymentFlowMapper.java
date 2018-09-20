package com.lhiot.oc.basic.mapper;

import com.lhiot.oc.basic.domain.PaymentFlow;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
* Description:支付状态流转记录Mapper类
* @author yijun
* @date 2018/09/20
*/
@Mapper
public interface PaymentFlowMapper {

    /**
    * Description:新增支付状态流转记录
    *
    * @param paymentFlow
    * @return
    * @author yijun
    * @date 2018/09/20 11:49:08
    */
    int create(PaymentFlow paymentFlow);

    /**
    * Description:根据id修改支付状态流转记录
    *
    * @param paymentFlow
    * @return
    * @author yijun
    * @date 2018/09/20 11:49:08
    */
    int updateById(PaymentFlow paymentFlow);

    /**
    * Description:根据ids删除支付状态流转记录
    *
    * @param ids
    * @return
    * @author yijun
    * @date 2018/09/20 11:49:08
    */
    int deleteByIds(java.util.List<String> ids);

    /**
    * Description:根据id查找支付状态流转记录
    *
    * @param id
    * @return
    * @author yijun
    * @date 2018/09/20 11:49:08
    */
    PaymentFlow selectById(Long id);

    /**
    * Description:查询支付状态流转记录列表
    *
    * @param paymentFlow
    * @return
    * @author yijun
    * @date 2018/09/20 11:49:08
    */
     List<PaymentFlow> pagePaymentFlows(PaymentFlow paymentFlow);


    /**
    * Description: 查询支付状态流转记录总记录数
    *
    * @param paymentFlow
    * @return
    * @author yijun
    * @date 2018/09/20 11:49:08
    */
    long pagePaymentFlowCounts(PaymentFlow paymentFlow);
}
