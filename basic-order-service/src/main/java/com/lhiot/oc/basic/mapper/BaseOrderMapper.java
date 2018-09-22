package com.lhiot.oc.basic.mapper;

import com.lhiot.oc.basic.model.BaseOrderInfo;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * @Author zhangfeng created in 2018/9/19 9:49
 **/
@Mapper
@Repository
public interface BaseOrderMapper {

    BaseOrderInfo insert(BaseOrderInfo baseOrderInfo);

    /**
     * 依据订单id修改订单状态
     * @param baseOrderInfo
     * @return
     */
    int updateOrderStatusById(BaseOrderInfo baseOrderInfo);

    /**
     * 依据订单code修改订单状态
     * @param baseOrderInfo
     * @return
     */
    int updateOrderStatusByCode(BaseOrderInfo baseOrderInfo);

    BaseOrderInfo findByCode(String code);

    BaseOrderInfo findById(Long id);
}
