package com.lhiot.oc.basic.feign.domain;

import lombok.Data;

/**
 * @author liuyo on 17.8.5.
 */
@Data
public class ShopParam{
    String stationName;
    String originShopId;
    String cityName;
    String areaName;
    String stationAddress;
    String contactName;
    double lng;
    double lat;
    String phone;

/*    public double[] bd09_To_Gcj02() {
        return bd09_To_Gcj02(lng, lat);
    }*/
}
