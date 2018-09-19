package com.lhiot.oc.basic.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Distance {

    private static double rad(double d) {
        return d * Math.PI / 180.0;
    }

    /**
     * 根据经纬度计算距离
     *
     * @param lat1 坐标1纬度
     * @param lng1 坐标1经度
     * @param lat2 坐标2纬度
     * @param lng2 坐标2经度
     * @return
     */
    public static BigDecimal getDistance(double lat1, double lng1, double lat2, double lng2) {

        double radLat1 = rad(lat1);
        double radLat2 = rad(lat2);
        double difference = radLat1 - radLat2;
        double difference2 = rad(lng1) - rad(lng2);
        double distance = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(difference / 2), 2) + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(difference2 / 2), 2)));
        distance = distance * 6378.137;
        BigDecimal bd = new BigDecimal(distance);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd;
    }
}
