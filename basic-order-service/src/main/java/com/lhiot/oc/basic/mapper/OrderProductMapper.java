package com.lhiot.oc.basic.mapper;

import com.lhiot.oc.basic.domain.OrderAssortment;
import com.lhiot.oc.basic.domain.OrderProduct;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 描述：服务类
 *
 * @author yijun
 * @date 2018-07-21
 */
@Mapper
public interface OrderProductMapper {

    int createInBatch(List<OrderProduct> orderProduct);

    //根据套餐ids查询套餐商品
    List<OrderProduct> findProductByAssortmentIds(Map<String, Object> map);

    //根据套餐查询套餐商品
    List<OrderProduct> findProductByAssortmentId(OrderAssortment orderAssortment);

    //根据订单id,套餐id及规格id修改订单商品的状态
    int updateByOrderIdAndAssortmentId(Map<String, Object> map);

    List<OrderProduct> findProductsByOrderIdAndStandId(Map<String, Object> map);

    int create(OrderProduct orderProduct);

    List<OrderProduct> findRefundProductsByOrderId(long orderId);

    int updateProductByStandardId(Map<String, Object> map);

    List<OrderProduct> list(OrderProduct orderProduct);

    List<OrderProduct> findOrderProductsByOrderId(long orderId);
}
