package com.lhiot.oc.basic.mapper;

import com.lhiot.oc.basic.model.BaseOrder;
import com.lhiot.oc.basic.model.BaseOrderInfo;
import com.lhiot.oc.basic.model.OrderDetailResult;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * @Author zhangfeng created in 2018/9/19 9:49
 **/
@Mapper
@Repository
public interface BaseOrderMapper {

    int insert(BaseOrder baseOrder);

    /**
     * 依据订单id修改订单状态
     *
     * @param baseOrder
     * @return
     */
    int updateOrderStatusById(BaseOrder baseOrder);

    /**
     * 依据订单code修改订单状态
     *
     * @param baseOrder
     * @return
     */
    int updateOrderStatusByCode(BaseOrder baseOrder);

    /**
     * 依据订单id修改订单状态及海鼎订单编码
     * @param baseOrderInfo
     * @return
     */
    int updateHdOrderCodeById(BaseOrderInfo baseOrderInfo);

    OrderDetailResult findByCode(String code);

    OrderDetailResult findById(Long id);
}
