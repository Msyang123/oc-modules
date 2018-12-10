package com.lhiot.oc.payment.mapper;

import com.lhiot.oc.payment.entity.Refund;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Mapper
@Repository
public interface RefundMapper {

    int completed(Map<String, Object> map);

    long historicalAmount(Long recordId);

    int insert(Refund refund);
}
