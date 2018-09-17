package com.lhiot.oc.basic.api;

import com.leon.microx.util.Converter;
import com.leon.microx.util.Jackson;
import com.leon.microx.util.Maps;
import com.lhiot.oc.basic.domain.DeliverTimeItem;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * @description: 系统时间接口
 * @author: Limiaojun
 * @create: 2018-06-14 16:01
 **/
@Api(description = "系统时间接口")
@RestController
@RequestMapping("/system/time")
public class SystemTimeApi {

    @GetMapping("/current")
    @ApiOperation(value = "获取服务器系统时间")
    public ResponseEntity<String> getTime() {
        return ResponseEntity.ok(String.valueOf(System.currentTimeMillis()));
    }

    @GetMapping("/deliver")
    @ApiOperation(value = "获取订单配送时间")
    public ResponseEntity<String> getDeliverTime(){
        Map<String,Map> timeResult=new HashMap<>();
        Date  current=new Date();
        String today = Converter.format(current,"yyyy-MM-dd");
        Calendar calendarOfTomorrow = Calendar.getInstance();
        calendarOfTomorrow.add(Calendar.DATE,1);
        String tomorrow = Converter.format(calendarOfTomorrow.getTime(),"yyyy-MM-dd");

        Date tonightBegin = Converter.date(today+" 21:31:00","yyyy-MM-dd HH:mm:ss");

        Date tonightEnd = Converter.date(tomorrow+" 08:29:59","yyyy-MM-dd HH:mm:ss");

        Date end = Converter.date(tomorrow+" 21:30:01","yyyy-MM-dd HH:mm:ss");

        Calendar calendar = Calendar.getInstance();
        String nextStartTime=null;
        String startTime=null;
        List<DeliverTimeItem> todayTimeList=new ArrayList<>();
        List<DeliverTimeItem> tomorrowTimeList=new ArrayList<>();
        boolean firstLable=true;
        while (true){
            startTime= Converter.format(calendar.getTime(),"yyyy-MM-dd HH")+":30:00";
            //每次加一个小时
            calendar.add(Calendar.HOUR_OF_DAY,1);

            //今天晚上9:30-明天早上8:30 不配送
            if(calendar.getTime().after(tonightBegin)&&calendar.getTime().before(tonightEnd)){
                continue;
            }
            //结束时间
            if(calendar.getTime().after(end)){
                break;
            }

            nextStartTime= Converter.format(calendar.getTime(),"yyyy-MM-dd HH")+":30:00";


            DeliverTimeItem deliverTimeItem = new DeliverTimeItem(
                    firstLable?"立即配送":(Converter.format(Converter.date(startTime,"yyyy-MM-dd HH:mm:ss"),"HH:mm")+
                            "-"+
                            Converter.format(Converter.date(nextStartTime,"yyyy-MM-dd HH:mm:ss"),"HH:mm")), startTime, nextStartTime);
            firstLable = false;
            if(calendar.getTime().before(tonightBegin)){
                todayTimeList.add(deliverTimeItem);
            }else{
                tomorrowTimeList.add(deliverTimeItem);
            }
        }
        timeResult.put("today", Maps.of("value",todayTimeList,"date", Converter.format(current,"MM-dd")));
        timeResult.put("tomorrow", Maps.of("value",tomorrowTimeList,"date", Converter.format(calendarOfTomorrow.getTime(),"MM-dd")));

        return ResponseEntity.ok(Jackson.json(timeResult));
    }
}
