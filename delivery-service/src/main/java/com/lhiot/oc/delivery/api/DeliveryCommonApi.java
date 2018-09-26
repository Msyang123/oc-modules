package com.lhiot.oc.delivery.api;

import com.leon.microx.support.result.Tips;
import com.leon.microx.util.*;
import com.lhiot.oc.delivery.domain.DeliverFeeParam;
import com.lhiot.oc.delivery.domain.DeliverTimeItem;
import com.lhiot.oc.delivery.feign.BasicDataService;
import com.lhiot.oc.delivery.feign.domain.Store;
import com.lhiot.oc.delivery.util.Distance;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;


@Slf4j
@RestController
@Api(description = "配送公共api")
@RequestMapping("/delivery")
public class DeliveryCommonApi {

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
    private final BasicDataService basicDataService;

    @Autowired
    public DeliveryCommonApi(BasicDataService basicDataService) {
        this.basicDataService = basicDataService;
    }

    @PostMapping("/query-fee")
    @ApiOperation("通用配送费计算接口")
    @ApiImplicitParam(paramType = "body", name = "deliverFeeParam", dataType = "DeliverFeeParam", required = true, value = "配送费计算传入参数")
    public ResponseEntity<Tips> queryDeliverFee(@RequestBody DeliverFeeParam deliverFeeParam) {
        //查询送货门店
        ResponseEntity response = basicDataService.findStoreById(deliverFeeParam.getStoreId(), deliverFeeParam.getApplicationType());
        if (response.getStatusCode().isError() || Objects.isNull(response.getBody())) {
            return ResponseEntity.badRequest().body(Tips.warn("查询门店信息失败"));
        }
        Store store = (Store) response.getBody();
        //距离换算
        BigDecimal distance = Distance.getDistance(
                store.getStorePosition().getLat(), store.getStorePosition().getLat(), deliverFeeParam.getTargetLat(), deliverFeeParam.getTargetLng()
        );
        log.info("门店到配送终点距离：{}", distance);
        int fee = 430;//配送费(分)
        Double weight = deliverFeeParam.getWeight();//基础费用

        //如果金额超过38元，免配送费
        if (deliverFeeParam.getOrderFee() >= 3800) {
            fee -= 430;
        }

        if (Calculator.ltOrEq(weight, 5.00)) {
            log.info("重量在5kg之内！");
        }

        if (Calculator.gt(weight, 5.00) && Calculator.ltOrEq(weight, 15.00)) {
            log.info("重量在5kg到15kg之内,每增加1KG加0.5元");
            fee += Calculator.toInt(Calculator.mul(Calculator.sub(weight, 5.0), 50));
        }

        if (Calculator.gt(distance.doubleValue(), 5.00)) {
            log.error("超过配送范围！{}", distance);
            return ResponseEntity.badRequest().body(Tips.of(-1, "超过配送范围！"));
        }

        if (Calculator.ltOrEq(distance.doubleValue(), 1.00)) {
            log.info("配送在1km之内，不加钱");
        }

        if (Calculator.gt(distance.doubleValue(), 1.00) && Calculator.ltOrEq(distance.doubleValue(), 2.00)) {
            log.info("配送在1km~2km之间，加1元");
            fee += 100;
        }

        if (Calculator.gt(distance.doubleValue(), 2.00) && Calculator.ltOrEq(distance.doubleValue(), 3.00)) {
            log.info("配送在2km~3km之间，加2元");
            fee += 200;
        }

        if (Calculator.gt(distance.doubleValue(), 3.00) && Calculator.ltOrEq(distance.doubleValue(), 4.00)) {
            log.info("配送在3km~4km之间，加4元");
            fee += 400;
        }

        String deliverTime = deliverFeeParam.getDeliveryTime();
        DeliverTimeItem deliverTimeItem = Jackson.object(deliverTime, DeliverTimeItem.class);

        log.info("配送时间{}", deliverTimeItem);

        String today = Converter.format(new Date(), "yyyy-MM-dd");
        Date deliverTimeBegin = Converter.date(today + " " + deliverTimeItem.getStartTime().split(" ")[1], "yyyy-MM-dd HH:mm:ss");
        Date deliverTimeEnd = Converter.date(today + " " + deliverTimeItem.getEndTime().split(" ")[1], "yyyy-MM-dd HH:mm:ss");

        Date rushHourBegin = Converter.date(today + " " + "11:00:00", "yyyy-MM-dd HH:mm:ss");
        Date rushHourEnd = Converter.date(today + " " + "13:00:00", "yyyy-MM-dd HH:mm:ss");

        if ((deliverTimeBegin.after(rushHourBegin) && deliverTimeBegin.before(rushHourEnd)) ||
                (deliverTimeEnd.after(rushHourBegin) && deliverTimeEnd.before(rushHourEnd))) {
            log.info("高峰时段加2元");
            fee += 200;
        }
        log.info("------最终配送费：{}", fee);
        return ResponseEntity.ok(Tips.of(0, String.valueOf(fee)));
    }

    @GetMapping("/time-list")
    @ApiOperation(value = "获取订单配送时间列表")
    public ResponseEntity<String> getDeliverTime() {
        Map<String, Map> timeResult = new HashMap<>();
        Date current = new Date();
        String today = Converter.format(current, "yyyy-MM-dd");
        Calendar calendarOfTomorrow = Calendar.getInstance();
        calendarOfTomorrow.add(Calendar.DATE, 1);
        String tomorrow = Converter.format(calendarOfTomorrow.getTime(), "yyyy-MM-dd");

        Date tonightBegin = Converter.date(today + " 21:31:00", "yyyy-MM-dd HH:mm:ss");

        Date tonightEnd = Converter.date(tomorrow + " 08:29:59", "yyyy-MM-dd HH:mm:ss");

        Date end = Converter.date(tomorrow + " 21:30:01", "yyyy-MM-dd HH:mm:ss");

        Calendar calendar = Calendar.getInstance();
        String nextStartTime;
        String startTime;
        List<DeliverTimeItem> todayTimeList = new ArrayList<>();
        List<DeliverTimeItem> tomorrowTimeList = new ArrayList<>();
        boolean firstLabel = true;
        while (true) {
            startTime = Converter.format(calendar.getTime(), "yyyy-MM-dd HH") + ":30:00";
            //每次加一个小时
            calendar.add(Calendar.HOUR_OF_DAY, 1);

            //今天晚上9:30-明天早上8:30 不配送
            if (calendar.getTime().after(tonightBegin) && calendar.getTime().before(tonightEnd)) {
                continue;
            }
            //结束时间
            if (calendar.getTime().after(end)) {
                break;
            }

            nextStartTime = Converter.format(calendar.getTime(), "yyyy-MM-dd HH") + ":30:00";
            String display = firstLabel
                    ? "立即配送"
                    : StringUtils.format("{}-{}",
                            Converter.format(Converter.date(startTime, "yyyy-MM-dd HH:mm:ss"), "HH:mm"),
                            Converter.format(Converter.date(nextStartTime, "yyyy-MM-dd HH:mm:ss"), "HH:mm")
                    );
            DeliverTimeItem deliverTimeItem = new DeliverTimeItem(display, startTime, nextStartTime);
            firstLabel = false;
            if (calendar.getTime().before(tonightBegin)) {
                todayTimeList.add(deliverTimeItem);
            } else {
                tomorrowTimeList.add(deliverTimeItem);
            }
        }
        timeResult.put("today", Maps.of("value", todayTimeList, "date", Converter.format(current, "MM-dd")));
        timeResult.put("tomorrow", Maps.of("value", tomorrowTimeList, "date", Converter.format(calendarOfTomorrow.getTime(), "MM-dd")));

        return ResponseEntity.ok(Jackson.json(timeResult));
    }
}
