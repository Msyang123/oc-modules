package com.lhiot.oc.delivery.repository;

import com.lhiot.oc.delivery.entity.DeliverFeeRule;
import com.lhiot.oc.delivery.model.DeliverFeeRulesResult;
import com.lhiot.oc.delivery.model.DeliverFeeSearchParam;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface DeliveryFeeRuleMapper {

    int insert(DeliverFeeRule deliverFeeRule);

    int updateById(DeliverFeeRule deliverFeeRule);

    List<DeliverFeeRulesResult> query(DeliverFeeSearchParam param);

    int deleteById(Long id);
}
