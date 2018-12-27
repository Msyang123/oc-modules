package com.lhiot.oc.delivery.repository;

import com.lhiot.oc.delivery.entity.DeliverFeeRuleDetail;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@Mapper
public interface DeliveryFeeRuleDetailMapper {

    int batchInsert(List<DeliverFeeRuleDetail> list);

    int updateBatch(List<DeliverFeeRuleDetail> list);

    DeliverFeeRuleDetail search(Map<String,Object> map);

    int deleteById(Long id);

    int deleteByRuleId(Long ruleId);

    void batchDelete(List<String> list);
}
