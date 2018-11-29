package com.lhiot.oc.delivery.feign;

import lombok.Data;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * 门店实体类
 */
@Data
@ToString
public class Store {

    /**
     * 门店id
     */
    private Long id;

    /**
     * 门店编码
     */
    private String storeCode;

    /**
     * 门店名称
     */
    private String storeName;

    /**
     * 门店地址
     */
    private String storeAddress;

    /**
     * 联系方式
     */
    private String storePhone;

    /**
     * 门店图片
     */
    private String storeImage;

    /**
     * 所属区域
     */
    private String storeArea;

    /**
     * 门店状态(0-未开启  1-开启)
     */
    private Status storeStatus;

    /**
     * 旗舰店ID
     */
    private Long storeFlagship;

    /**
     * 门店类型：00-普通门店  01-旗舰店
     */
    private Type storeType;

    /**
     * 门店视频
     */
    private String videoUrl;

    /**
     * 直播开始时间
     */
    private String beginTime;

    /**
     * 直播结束时间
     */
    private String endTime;

    /**
     * 录播地址
     */
    private String tapeUrl;
    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * 经度
     */
    private BigDecimal longitude;

    private String distance;

    public enum Status {

        ENABLED("营业"),

        DISABLED("未营业");

        @Getter
        private String description;

        Status(String description) {
            this.description = description;
        }
    }

    public enum Type {

        ORDINARY_STORE("普通门店"),

        FLAGSHIP_STORE("旗舰店");

        @Getter
        private String description;

        Type(String description) {
            this.description = description;
        }
    }
}
