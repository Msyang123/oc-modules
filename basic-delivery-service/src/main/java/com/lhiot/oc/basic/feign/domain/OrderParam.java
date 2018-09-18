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
    private int cargoPrice;//分
    private String receiverName;
    private String receiverAddress;
    private double lat;//经度
    private double lng;//纬度
    private String callback;
    private String receiverPhone;
    private String receiverTel;
    private String info;
    private int cargoNum;
    private String originMark;//订单来源标示（该字段可以显示在达达app订单详情页面，只支持字母，最大长度为10）
    private String originMarkNo;//订单来源编号（该字段可以显示在达达app订单详情页面，支持字母和数字，最大长度为30）
    private double cargoWeight=0.0;
}
