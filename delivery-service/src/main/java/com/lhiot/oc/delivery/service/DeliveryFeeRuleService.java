package com.lhiot.oc.delivery.service;

import com.leon.microx.util.BeanUtils;
import com.leon.microx.util.Calculator;
import com.leon.microx.util.StringUtils;
import com.leon.microx.web.result.Tips;
import com.lhiot.oc.delivery.entity.DeliverFeeRule;
import com.lhiot.oc.delivery.entity.DeliverFeeRuleDetail;
import com.lhiot.oc.delivery.entity.UpdateWay;
import com.lhiot.oc.delivery.feign.BasicDataService;
import com.lhiot.oc.delivery.feign.Store;
import com.lhiot.oc.delivery.model.DeliverFeeRuleParam;
import com.lhiot.oc.delivery.repository.DeliveryFeeRuleDetailMapper;
import com.lhiot.oc.delivery.repository.DeliveryFeeRuleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author zhangfeng create in 12:27 2018/12/11
 */
@Service
@Slf4j
@Transactional
public class DeliveryFeeRuleService {
    public static final double MAX_DELIVERY_WEIGHT = 10.00;
    private DeliveryFeeRuleMapper deliveryFeeRuleMapper;
    private DeliveryFeeRuleDetailMapper deliveryFeeRuleDetailMapper;
    private BasicDataService basicDataService;

    public DeliveryFeeRuleService(DeliveryFeeRuleMapper deliveryFeeRuleMapper, DeliveryFeeRuleDetailMapper deliveryFeeRuleDetailMapper, BasicDataService basicDataService) {
        this.deliveryFeeRuleMapper = deliveryFeeRuleMapper;
        this.deliveryFeeRuleDetailMapper = deliveryFeeRuleDetailMapper;
        this.basicDataService = basicDataService;
    }

    /**
     * 添加计算配送费规则
     *
     * @param param 规则参数
     * @return boolean
     */
    public boolean create(DeliverFeeRuleParam param) {
        DeliverFeeRule deliverFeeRule = new DeliverFeeRule();
        BeanUtils.of(deliverFeeRule).populate(param);
        deliverFeeRule.setCreateAt(Date.from(Instant.now()));
        deliverFeeRule.setUpdateAt(Date.from(Instant.now()));
        boolean flag = deliveryFeeRuleMapper.insert(deliverFeeRule) == 1;
        if (flag) {
            if (CollectionUtils.isEmpty(param.getDetailList())) {
                return true;
            }
            param.getDetailList().forEach(ruleDetail -> ruleDetail.setDeliveryFeeRuleId(deliverFeeRule.getId()));
            flag = deliveryFeeRuleDetailMapper.batchInsert(param.getDetailList()) > 0;
        }
        return flag;
    }

    /**
     * 修改配送费规则
     *
     * @param param 规则参数
     * @return boolean
     */
    public void updateRules(DeliverFeeRuleParam param) {
        DeliverFeeRule deliverFeeRule = new DeliverFeeRule();
        BeanUtils.of(deliverFeeRule).populate(param);
        deliverFeeRule.setUpdateAt(Date.from(Instant.now()));
        deliveryFeeRuleMapper.updateById(deliverFeeRule);

        List<DeliverFeeRuleDetail> insertList = param.getDetailList().stream().filter(detail -> Objects.equals(detail.getUpdateWay(), UpdateWay.INSERT))
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(insertList)) {
            deliveryFeeRuleDetailMapper.batchInsert(insertList);
        }
        List<DeliverFeeRuleDetail> updateList = param.getDetailList().stream().filter(detail -> Objects.equals(detail.getUpdateWay(), UpdateWay.UPDATE))
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(updateList)) {
            deliveryFeeRuleDetailMapper.updateBatch(updateList);
        }
    }

    public Optional<Store> store(Long storeId, String applicationType) {
        ResponseEntity response = basicDataService.findStoreById(storeId, applicationType);
        if (response.getStatusCode().isError() || Objects.isNull(response.getBody())) {
            return Optional.empty();
        }
        return Optional.of((Store) response.getBody());
    }

    /**
     * 计算配送费
     *
     * @param weight     订单重量
     * @param ruleDetail 规则详情
     * @return Tips
     */
    public Tips<Integer> fee(double weight, DeliverFeeRuleDetail ruleDetail) {
        if (Calculator.gt(weight, MAX_DELIVERY_WEIGHT)) {
            return Tips.warn("超出配送重量");
        }
        double firstWeight = ruleDetail.getFirstWeight().doubleValue();
        double deliveryFee = ruleDetail.getFirstFee();
        if (Calculator.lt(firstWeight, weight)) {
            double overstep = firstWeight - weight;
            double remainder = Calculator.div(overstep, ruleDetail.getAdditionalWeight().doubleValue());
            remainder = Math.ceil(remainder);
            deliveryFee += remainder * ruleDetail.getAdditionalFee();
        }
        Tips<Integer> tips = Tips.empty();
        tips.setData((int) deliveryFee);
        return tips;
    }

    public boolean deleteRule(String ids) {
        List<String> idList = Arrays.asList(StringUtils.tokenizeToStringArray(ids,","));
        boolean flag = deliveryFeeRuleMapper.deleteById(idList) > 0;
        if (flag) {
            flag = deliveryFeeRuleDetailMapper.batchDeleteByRuleId(idList) > 0;
        }
        return flag;
    }
}
