package com.lhiot.oc.order.mapper;

import com.lhiot.oc.order.model.BaseOrder;
import com.lhiot.oc.order.model.OrderDetailResult;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * zhangfeng created in 2018/9/19 9:49
 **/
@Mapper
@Repository
public interface BaseOrderMapper {

    int insert(BaseOrder baseOrder);

    /**
     * 依据订单id修改订单状态
     *
     * @param baseOrder BaseOrder
     * @return int
     */
    int updateOrderStatusById(BaseOrder baseOrder);

    /**
     * 依据订单code修改订单状态
     *
     * @param baseOrder BaseOrder
     * @return int
     */
    int updateOrderStatusByCode(BaseOrder baseOrder);

    /**
     * 依据订单id修改订单状态及海鼎订单编码
     * @param baseOrder BaseOrder
     * @return int
     */
    int updateHdOrderCodeById(BaseOrder baseOrder);

    OrderDetailResult findByCode(String code);

    OrderDetailResult findById(Long id);
}
