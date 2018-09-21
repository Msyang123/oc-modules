package com.lhiot.oc.basic.mapper;

import com.lhiot.oc.basic.model.OrderProduct;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Author zhangfeng created in 2018/9/19 10:51
 **/
@Mapper
@Repository
public interface OrderProductMapper {

    OrderProduct batchInsert(List<OrderProduct> orderProducts);
}
