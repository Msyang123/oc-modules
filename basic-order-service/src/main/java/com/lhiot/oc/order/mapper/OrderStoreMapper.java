package com.lhiot.oc.order.mapper;

import com.lhiot.oc.order.model.OrderStore;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * @author zhangfeng created in 2018/9/19 12:18
 **/
@Mapper
@Repository
public interface OrderStoreMapper {

    int insert(OrderStore orderStore);

    OrderStore findByHdOrderCode(String hdOrderCode);
}
