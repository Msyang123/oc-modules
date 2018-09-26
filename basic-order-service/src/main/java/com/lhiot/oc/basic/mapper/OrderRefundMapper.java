package com.lhiot.oc.basic.mapper;

import com.lhiot.oc.basic.model.OrderRefund;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * @Author zhangfeng created in 2018/9/25 17:11
 **/
@Mapper
@Repository
public interface OrderRefundMapper {

    int insert(OrderRefund orderRefund);
}
