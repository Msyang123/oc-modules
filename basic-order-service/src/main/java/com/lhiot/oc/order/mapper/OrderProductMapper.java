package com.lhiot.oc.order.mapper;

import com.lhiot.oc.order.entity.OrderProduct;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * @author zhangfeng created in 2018/9/19 10:51
 **/
@Mapper
@Repository
public interface OrderProductMapper {

    int batchInsert(List<OrderProduct> orderProducts);

    List<OrderProduct> selectOrderProductsByOrderId(long orderId);

    int updateOrderProductByIds(Map<String, Object> map);

    List<OrderProduct> selectOrderProductsByIds(List<String> idList);

}
