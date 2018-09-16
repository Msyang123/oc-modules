package com.lhiot.oc.basic.api;

import com.leon.microx.support.result.Tips;
import com.leon.microx.util.Calculator;
import com.leon.microx.util.Converter;
import com.leon.microx.util.Jackson;
import com.lhiot.oc.basic.domain.DeliverFeeParam;
import com.lhiot.oc.basic.domain.DeliverTimeItem;
import com.lhiot.oc.basic.feign.BaseDataServiceFeign;
import com.lhiot.oc.basic.feign.domain.Store;
import com.lhiot.oc.basic.util.Distance;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;


@Slf4j
@RestController
@Api("配送运费api")
@RequestMapping("/delivery-fee")
public class DeliveryFeeApi {

    /**
     * 配送费计算
     * 通过蜂鸟的费用计算公式计算费用
     * 4.3 + 距离加价 + 重量加价 + 时段加价 = 蜂鸟配送费
     * 配送距离 加价规则（元/单）[0-1)km 0元 [1-2)km 1元 [2-3)km 2元 [3-4]km 4元
     * 重量 加价规则（元/单）[0-5] kg 0, (5-15] kg 每增加1KG加0.5元
     * 高峰时段 2元 11:00-13:00 22:00-02:00
     * 1、即时单以下单时间为区间判断的时间节点；
     * 2、预约单以要求送到时间为区间判断的时间节点；
     * 3、考虑到消费场景的可行性，暂不建议接02:00-9:00的订单
     * 4km以内，15kg以内才配送
     * 退货费用：非乙方及其配送团队原因产生的退货，甲方确认退货后要求乙方即时退回的，乙方收取甲方退货费为该订单基本配送费用的50%。
     * 基础费用 4.3d 测试费用0.01d
     */
    private final BaseDataServiceFeign baseDataServiceFeign;
    @Autowired
    public DeliveryFeeApi(BaseDataServiceFeign baseDataServiceFeign){

        this.baseDataServiceFeign = baseDataServiceFeign;
    }
    @PostMapping("/query")
    @ApiOperation("通用配送费计算接口")
    @ApiImplicitParam(paramType = "body", name = "deliverFeeParam", dataType = "DeliverFeeParam", required = true, value = "配送费计算传入参数")
    public ResponseEntity<Tips> queryDeliverFee(DeliverFeeParam deliverFeeParam){
        //查询送货门店
        ResponseEntity<Store> storeResponseEntity = baseDataServiceFeign.findStoresId(deliverFeeParam.getStoreId(),deliverFeeParam.getApplicationType());
        if(Objects.isNull(storeResponseEntity)||!storeResponseEntity.getStatusCode().is2xxSuccessful()){
            return ResponseEntity.badRequest().body(Tips.of(-1,"查询门店信息失败"));
        }

        Store store=storeResponseEntity.getBody();
        //距离换算
        BigDecimal distance = Distance.getDistance(store.getStorePosition().getStoreCoordx(),store.getStorePosition().getStoreCoordy(),deliverFeeParam.getTargetCoordx(),deliverFeeParam.getTargetCoordy());

        log.info("门店到配送终点距离：{}",distance);
        int fee=430;//配送费(分)
        Double weight = deliverFeeParam.getWeight();//基础费用

        //如果金额超过38元，免配送费
        if(deliverFeeParam.getOrderFee()>=3800){
            fee-=fee-430;
        }

        if(Calculator.ltOrEq(weight,5.00)){
            log.info("重量在5kg之内！");
        }

        if(Calculator.gt(weight,5.00)&&Calculator.ltOrEq(weight,15.00)){
            log.info("重量在5kg到15kg之内,每增加1KG加0.5元");
            fee += Calculator.toInt(Calculator.mul(Calculator.sub(weight,5.0),50));
        }

        if(Calculator.gt(distance.doubleValue(),5.00)){
            log.error("超过配送范围！{}",distance);
            return ResponseEntity.badRequest().body(Tips.of(-1,"超过配送范围！"));
        }

        if(Calculator.ltOrEq(distance.doubleValue(),1.00)){
            log.info("配送在1km之内，不加钱");
        }

        if(Calculator.gt(distance.doubleValue(),1.00)&&Calculator.ltOrEq(distance.doubleValue(),2.00)){
            log.info("配送在1km~2km之间，加1元");
            fee+=100;
        }

        if(Calculator.gt(distance.doubleValue(),2.00)&&Calculator.ltOrEq(distance.doubleValue(),3.00)){
            log.info("配送在2km~3km之间，加2元");
            fee+=200;
        }

        if(Calculator.gt(distance.doubleValue(),3.00)&&Calculator.ltOrEq(distance.doubleValue(),4.00)){
            log.info("配送在3km~4km之间，加4元");
            fee+=400;
        }

        String deliverTime = deliverFeeParam.getDeliveryTime();
        DeliverTimeItem deliverTimeItem = Jackson.object(deliverTime,DeliverTimeItem.class);

        log.info("配送时间{}",deliverTimeItem);

        Date  current=new Date();
        String today = Converter.format(current,"yyyy-MM-dd");

        Date rushHourBegin = Converter.date(today+" 11:00:00","yyyy-MM-dd HH:mm:ss");
        Date rushHourEnd = Converter.date(today+" 13:00:00","yyyy-MM-dd HH:mm:ss");

        if(current.after(rushHourBegin)&&current.before(rushHourEnd)){
            log.info("高峰时段加2元");
            fee += 200;
        }
        log.info("------最终配送费：{}",fee);
        return ResponseEntity.ok(Tips.of(0,String.valueOf(fee)));
    }
}
