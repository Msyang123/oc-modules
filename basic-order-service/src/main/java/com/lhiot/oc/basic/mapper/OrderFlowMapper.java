package com.lhiot.oc.basic.mapper;

import com.lhiot.oc.basic.domain.OrderFlow;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
* 描述：服务类
* @author yijun
* @date 2018-07-21
*/
@Mapper
public interface OrderFlowMapper {

    int create(OrderFlow orderFlow);

    //根据订单id查询
    List<OrderFlow> flowByOrderId(Long orderId);
}
