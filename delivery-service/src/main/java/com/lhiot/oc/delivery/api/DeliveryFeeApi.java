package com.lhiot.oc.delivery.api;

import com.leon.microx.util.Calculator;
import com.leon.microx.util.Maps;
import com.leon.microx.util.Position;
import com.leon.microx.web.result.Pages;
import com.leon.microx.web.result.Tips;
import com.leon.microx.web.swagger.ApiParamType;
import com.lhiot.oc.delivery.entity.DeliverFeeRuleDetail;
import com.lhiot.oc.delivery.feign.Store;
import com.lhiot.oc.delivery.model.DeliverFeeQuery;
import com.lhiot.oc.delivery.model.DeliverFeeRuleParam;
import com.lhiot.oc.delivery.model.DeliverFeeRulesResult;
import com.lhiot.oc.delivery.model.DeliverFeeSearchParam;
import com.lhiot.oc.delivery.repository.DeliveryFeeRuleDetailMapper;
import com.lhiot.oc.delivery.repository.DeliveryFeeRuleMapper;
import com.lhiot.oc.delivery.service.DeliveryFeeRuleService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author zhangfeng create in 11:00 2018/12/11
 */
@RestController
@Slf4j
public class DeliveryFeeApi {

    private static final  Integer DEFAULT_DELIVER_FEE = 500;//无配送模板默认配送费为5元
    private DeliveryFeeRuleService ruleService;
    private DeliveryFeeRuleMapper deliveryFeeRuleMapper;
    private DeliveryFeeRuleDetailMapper deliveryFeeRuleDetailMapper;

    public DeliveryFeeApi(DeliveryFeeRuleService ruleService, DeliveryFeeRuleMapper deliveryFeeRuleMapper, DeliveryFeeRuleDetailMapper deliveryFeeRuleDetailMapper) {
        this.ruleService = ruleService;
        this.deliveryFeeRuleMapper = deliveryFeeRuleMapper;
        this.deliveryFeeRuleDetailMapper = deliveryFeeRuleDetailMapper;
    }

    @ApiOperation("添加配送费计算规则")
    @ApiImplicitParam(paramType = ApiParamType.BODY, name = "deliverFeeRuleParam", value = "添加配送费规则入参", dataType = "DeliverFeeRuleParam", required = true)
    @PostMapping("/delivery-fee-rule")
    public ResponseEntity createRule(@RequestBody DeliverFeeRuleParam deliverFeeRuleParam) {
        if (!ruleService.create(deliverFeeRuleParam)) {
            return ResponseEntity.badRequest().body("添加规则错误");
        }
        return ResponseEntity.ok().build();
    }

    @ApiOperation("修改配送费计算规则")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = ApiParamType.PATH, name = "id", value = "配送费规则Id", dataType = "Long", required = true),
            @ApiImplicitParam(paramType = ApiParamType.BODY, name = "deliverFeeRuleParam", value = "需要修改的配送费规则模板以及详细规则", dataType = "DeliverFeeRuleParam", required = true)
    })
    @PutMapping("/delivery-fee-rule/{id}")
    public ResponseEntity updateRules(@PathVariable("id") Long ruleId, @RequestBody DeliverFeeRuleParam deliverFeeRuleParam) {
        deliverFeeRuleParam.setId(ruleId);
        if (!CollectionUtils.isEmpty(deliverFeeRuleParam.getDeleteIds())) {
            deliveryFeeRuleDetailMapper.batchDelete(deliverFeeRuleParam.getDeleteIds());
        }
        ruleService.updateRules(deliverFeeRuleParam);
        return ResponseEntity.ok().build();
    }

    @ApiOperation("后台管理查询配送费规则列表")
    @ApiImplicitParam(paramType = ApiParamType.BODY, name = "param", value = "查询条件", dataType = "DeliverFeeSearchParam", required = true)
    @PostMapping("/delivery-fee-rule/query")
    public ResponseEntity query(@RequestBody DeliverFeeSearchParam param) {
        List<DeliverFeeRulesResult> resultList = deliveryFeeRuleMapper.query(param);
        int count = 0;
        if (Objects.nonNull(param.getStartRows())) {
            count = deliveryFeeRuleMapper.count(param);
        }
        return ResponseEntity.ok(Pages.of(count, resultList));

    }

    @ApiOperation("根据配送费规则模板Id删除")
    @ApiImplicitParam(paramType = ApiParamType.PATH, name = "ids", value = "配送费规则Id", dataType = "String", required = true)
    @DeleteMapping("/delivery-fee-rule/{ids}")
    public ResponseEntity deleteRule(@PathVariable("ids") String ids) {
        return ruleService.deleteRule(ids) ? ResponseEntity.ok().build() : ResponseEntity.badRequest().body("删除失败");
    }

    @ApiOperation("计算配送费")
    @ApiImplicitParam(paramType = ApiParamType.BODY, name = "feeQuery", value = "配送费计算传入参数", dataType = "DeliverFeeQuery", required = true)
    @PostMapping("/delivery/fee/search")
    public ResponseEntity search(@RequestBody DeliverFeeQuery feeQuery) {
        Optional<Store> store = ruleService.store(feeQuery.getStoreId(), feeQuery.getApplicationType());
        if (!store.isPresent()) {
            return ResponseEntity.badRequest().body("查询门店信息失败！");
        }
        //传入经纬度是否需要转换坐标系
        if (feeQuery.getCoordinateSystem().isNeedConvert()) {
            Position.BD09 bd09 = Position.baidu(feeQuery.getTargetLng(), feeQuery.getTargetLat());
            Position.GCJ02 amap = Position.GCJ02.of(bd09);
            feeQuery.setTargetLng(amap.getLongitude());
            feeQuery.setTargetLat(amap.getLatitude());
        }
        double distance = Position.base(store.get().getLongitude().doubleValue(),store.get().getLatitude().doubleValue())
                .distance(Position.base(feeQuery.getTargetLng(), feeQuery.getTargetLat())).doubleValue();
        distance = Calculator.div(distance, 1000.0);
        DeliverFeeRuleDetail ruleDetail = deliveryFeeRuleDetailMapper.search(Maps.of("orderFee", feeQuery.getOrderFee(), "distance", distance,
                "deliveryAtType", feeQuery.getDeliverAtType()));
        if (Objects.isNull(ruleDetail)) {
            return ResponseEntity.ok().body(Maps.of("fee", DEFAULT_DELIVER_FEE));
        }
        Tips<Integer> tips = ruleService.fee(feeQuery.getWeight(), ruleDetail);
        if (tips.err()) {
            return ResponseEntity.badRequest().body("超出配送重量");
        }
        return ResponseEntity.ok().body(Maps.of("fee", tips.getData()));
    }
}
