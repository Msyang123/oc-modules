package com.lhiot.oc.basic.mapper;

import com.lhiot.oc.basic.domain.OrderRefund;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
* 描述：服务类
* @author yijun
* @date 2018-07-21
*/
@Mapper
public interface OrderRefundMapper {
	
    int create(OrderRefund orderRefund);

    int updateById(OrderRefund orderRefund);

    int deleteByIds(List<String> ids);

    List<OrderRefund> list(OrderRefund orderRefund);

    List<OrderRefund> page(OrderRefund param);

    int pageQueryCount(OrderRefund param);

    OrderRefund findById(Long id);
}
