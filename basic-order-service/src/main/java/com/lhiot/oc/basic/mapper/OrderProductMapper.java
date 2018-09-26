package com.lhiot.oc.basic.mapper;

import com.lhiot.oc.basic.model.OrderProduct;
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

    List<OrderProduct> findOrderProductsByOrderId(long orderId);

    int updateOrderProductByIds(Map<String, Object> map);


}
