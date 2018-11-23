package com.lhiot.oc.order.mapper;

import com.lhiot.oc.order.entity.OrderFlow;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 描述：订单状态流转记录表
 *
 * @author yijun
 * 2018-07-21
 */
@Mapper
@Repository
public interface OrderFlowMapper {

    int create(OrderFlow orderFlow);

    //根据订单id查询
    List<OrderFlow> selectFlowByOrderId(Long orderId);
}
