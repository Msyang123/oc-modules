package com.lhiot.oc.order.mapper;

import com.lhiot.oc.order.entity.OrderRefund;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * @uthor zhangfeng created in 2018/9/25 17:11
 **/
@Mapper
@Repository
public interface OrderRefundMapper {

    int insert(OrderRefund orderRefund);
}
