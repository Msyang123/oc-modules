package com.lhiot.oc.delivery.api.calculator;

import lombok.Data;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;

/**
 *  时段
 * @author Leon (234239150@qq.com) created in 17:22 18.11.10
 */
@Data
@ToString
public class PeriodOfTime {
    private LocalDate day = LocalDate.now();
    private LocalTime begin;
    private LocalTime end;

    private PeriodOfTime(LocalTime begin, LocalTime end) {
        this.begin = begin;
        this.end = end;
    }

    public static PeriodOfTime of(Date begin, Date end) {
        return new PeriodOfTime(LocalTime.from(begin.toInstant()), LocalTime.from(end.toInstant()));
    }

    public static PeriodOfTime of(String begin, String end) {
        return new PeriodOfTime(LocalTime.parse(begin), LocalTime.parse(end));
    }

    public boolean isRushHour() {
        PeriodOfTime period = PeriodOfTime.of("11:00:00", "13:00:00");
        boolean beginTimeInTheRushHour = this.begin.isAfter(period.begin) && this.begin.isBefore(period.end);
        boolean endTimeInTheRushHour = this.end.isAfter(period.begin) && this.end.isBefore(period.end);
        return beginTimeInTheRushHour || endTimeInTheRushHour;
    }
}
