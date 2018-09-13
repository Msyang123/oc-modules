package com.lhiot.oc.basic.mapper;

import com.lhiot.oc.basic.domain.OrderAssortment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 鲜果师 订单套餐
 *
 * @author lynn
 */
@Mapper
public interface OrderAssortmentMapper {
    int createInbatch(List<OrderAssortment> orderAssortment);

    List<OrderAssortment> findByOrderId(Long orderId);


    /**
     * 批量修改订单套餐的状态
     *
     * @param map
     * @return
     */
    int updateByOrderIdAndAssortmentId(Map<String, Object> map);

    List<OrderAssortment> list(OrderAssortment orderAssortment);

    //根据套餐id及订单id查询订单关联的套餐
    List<OrderAssortment> findByOrderIdAndassormentId(Map<String, Object> map);
}
