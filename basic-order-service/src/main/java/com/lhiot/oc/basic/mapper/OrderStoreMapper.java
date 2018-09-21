package com.lhiot.oc.basic.mapper;

import com.lhiot.oc.basic.model.OrderStore;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Repository;

/**
 * @Author zhangfeng created in 2018/9/19 12:18
 **/
@Mapper
@Repository
public interface OrderStoreMapper {

    int insert(OrderStore orderStore);
}
