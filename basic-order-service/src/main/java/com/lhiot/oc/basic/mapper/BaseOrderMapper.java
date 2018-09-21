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
}
