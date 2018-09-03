package com.lhiot.oc.basic.mapper;

import com.lhiot.oc.basic.domain.TransferOrderLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TransferOrderLogMapper {

    int create(TransferOrderLog transferOrderLog);
}
