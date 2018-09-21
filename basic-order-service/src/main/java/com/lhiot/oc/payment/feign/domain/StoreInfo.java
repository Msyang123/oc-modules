package com.lhiot.oc.delivery.feign.domain;

import lombok.Data;

@Data
public class StoreInfo {
    private String storeCode;
    private Long storeId;
    private String storeName;
    private String storeImage;
    private String storePhone;
    private String storeAddress;
    //经度
    private String storeCoordy;
    //纬度
    private String storeCoordx;

}
