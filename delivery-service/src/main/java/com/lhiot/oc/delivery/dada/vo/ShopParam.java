package com.lhiot.oc.delivery.dada.vo;

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
}
