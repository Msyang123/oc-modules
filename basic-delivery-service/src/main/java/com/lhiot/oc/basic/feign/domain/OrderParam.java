package com.lhiot.oc.basic.feign.domain;

import lombok.Data;

/**
 * @author liuyo on 17.8.5.
 */
@Data
public class OrderParam{

    private String shopNo;
    private String originId;
    private String cityCode;
    private int cargoPrice;
    private String receiverName;
    private String receiverAddress;
    private double lat;
    private double lng;
    private String callback;
    private String receiverPhone;
    private String receiverTel;
    private String info;
    private int cargoNum;
    private String originMark;
    private String originMarkNo;
}
