package com.lhiot.oc.delivery.api.calculator;

import com.leon.microx.util.Calculator;
import com.leon.microx.util.Pair;
import com.leon.microx.util.Position;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Optional;

/**
 * 配送费计算
 *
 * @author Leon (234239150@qq.com) created in 17:21 18.11.10
 */
@Slf4j
public class FeeCalculator {
    // 公司定的默认配送费，配送商品超过38元可免除
    private static final int DEFAULT_DELIVERY_FEE = 430;

    // 满免阈值（即满多少钱可以免配送费）
    private static final int FREE_DELIVERY_FEE_THRESHOLD = 3800;

    // 最大配送范围
    public static final double MAX_DELIVERY_RANGE = 5.00;

    // 最终配送费用
    private long ultimateFee = DEFAULT_DELIVERY_FEE;

    // 是否超过最大配送范围
    private boolean outOfBounds = false;

    /**
     * 设置配送计算器初始参数
     *
     * @param orderFee 配送单金额
     * @param weight   配送重量
     * @return DeliveryCalculator instance
     */
    public static FeeCalculator of(long orderFee, double weight) {
        FeeCalculator calculator = new FeeCalculator();
        if (orderFee >= FREE_DELIVERY_FEE_THRESHOLD) {
            calculator.ultimateFee = 0;
            log.info("订单金额：{}, 可免费配送。", orderFee);
        }
        if (Calculator.ltOrEq(weight, 5.00)) {
            calculator.ultimateFee = 0;
            log.info("配送重量：{}, 可免费配送。", weight);
        }

        if (Calculator.gt(weight, 5.00) && Calculator.ltOrEq(weight, 15.00)) {
            calculator.ultimateFee += Calculator.toInt(Calculator.mul(Calculator.sub(weight, 5.0), 50));
            log.info("重量在5kg到15kg之内,每增加1KG加0.5元");
        }
        return calculator;
    }

    /**
     * 设置配送距离
     *
     * @param source 从哪个经纬度开始配送
     * @param target 送到哪个经纬度
     * @return this
     */
    public FeeCalculator distance(Position.Coordinate source, Position.Coordinate target) {
        double distance = source.distance(target).doubleValue();
        if (Calculator.gt(distance, MAX_DELIVERY_RANGE)) {
            this.outOfBounds = true;
            log.error("超过配送范围！{}", distance);
            return this;
        }

        if (Calculator.ltOrEq(distance, 1.00)) {
            log.info("配送在1km之内，不加钱");
            return this;
        }

        if (Calculator.gt(distance, 1.00) && Calculator.ltOrEq(distance, 2.00)) {
            this.ultimateFee += 100;
            log.info("配送在1km~2km之间，加1元");
        }

        if (Calculator.gt(distance, 1.00) && Calculator.ltOrEq(distance, 2.00)) {
            this.ultimateFee += 100;
            log.info("配送在1km~2km之间，加1元");
        }

        if (Calculator.gt(distance, 2.00) && Calculator.ltOrEq(distance, 3.00)) {
            this.ultimateFee += 200;
            log.info("配送在2km~3km之间，加2元");
        }

        if (Calculator.gt(distance, 3.00) && Calculator.ltOrEq(distance, 4.00)) {
            this.ultimateFee += 400;
            log.info("配送在3km~4km之间，加4元");
        }
        return this;
    }

    /**
     * 设置配送时间段
     *
     * @param begin 配送开始时间
     * @param end 配送结束时间
     * @return this
     */
    public FeeCalculator period(Date begin, Date end) {
        PeriodOfTime time = PeriodOfTime.of(begin, end);
        if (!this.outOfBounds && time.isRushHour()) {
            this.ultimateFee += 200;
            log.info("高峰时段加2元");
        }
        return this;
    }

    /**
     * 完成计算。如果超出配送范围，将返回空值，由外部处理
     *
     * @return Optional
     */
    public Optional<Long> completed() {
        if (this.outOfBounds) {
            return Optional.empty();
        }
        return Optional.of(this.ultimateFee);
    }

    @FunctionalInterface
    public interface PairSupplier<O, T> {
        Pair<O, T> get();
    }
}
