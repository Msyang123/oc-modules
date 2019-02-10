package com.lhiot.oc.order.mapper;

import com.lhiot.oc.order.entity.BaseOrder;
import com.lhiot.oc.order.model.BaseOrderParam;
import com.lhiot.oc.order.model.OrderDetailResult;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

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
     * 依据订单id修改订单状态及海鼎订单编码
     *
     * @param baseOrder BaseOrder
     * @return int
     */
    int updateHdOrderCodeById(BaseOrder baseOrder);

    OrderDetailResult selectByCode(String code);

    OrderDetailResult selectById(Long id);

    List<OrderDetailResult> selectListByUserIdAndParam(Map<String, Object> map);

    int updateStatusByCode(Map<String, Object> map);

    int updateStatusToReturning(String orderCode);

    /**
     * 查询订单列表
     *
     * @param param 参数
     * @return 订单信息列表
     */
    List<OrderDetailResult> findList(BaseOrderParam param);

    /**
     * 查询订单总数
     *
     * @param param 参数
     * @return 总数
     */
    int findCount(BaseOrderParam param);

    int updateStatusByPayId(Map<String,Object> map);
}
