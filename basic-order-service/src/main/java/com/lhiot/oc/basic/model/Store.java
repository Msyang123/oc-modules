package com.lhiot.oc.basic.model;

import com.lhiot.oc.basic.model.type.StoreStatus;
import com.lhiot.oc.basic.model.type.StoreType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
* Description:门店实体类
* @author Limiaojun
* @date 2018/06/04
*/
@Data
@ToString(callSuper = true)
@ApiModel
@NoArgsConstructor
public class Store{

    /**
    *门店id
    */
    @ApiModelProperty(notes = "门店id", dataType = "Long")
    private Long id;

    /**
    *门店编码
    */
    @ApiModelProperty(notes = "门店编码", dataType = "String")
    private String storeCode;

    /**
    *门店名称
    */
    @ApiModelProperty(notes = "门店名称", dataType = "String")
    private String storeName;

    /**
    *门店地址
    */
    @ApiModelProperty(notes = "门店地址", dataType = "String")
    private String storeAddress;

    /**
    *联系方式
    */
    @ApiModelProperty(notes = "联系方式", dataType = "String")
    private String storePhone;

    /**
    *门店图片
    */
    @ApiModelProperty(notes = "门店图片", dataType = "String")
    private String storeImage;

    /**
    *所属区域
    */
    @ApiModelProperty(notes = "所属区域", dataType = "String")
    private String storeArea;

    /**
    *门店状态(0-未开启  1-开启)
    */
    @ApiModelProperty(notes = "门店状态 ENABLED(\"营业\"),DISABLED(\"未营业\");", dataType = "StoreStatusEnum")
    private StoreStatus storeStatus;

    /**
    *旗舰店ID
    */
    @ApiModelProperty(notes = "旗舰店ID", dataType = "Long")
    private Long storeFlagship;

    /**
    *门店类型：00-普通门店  01-旗舰店
    */
    @ApiModelProperty(notes = "门店类型：ORDINARY_STORE(\"普通门店\"),FLAGSHIP_STORE (\"旗舰店\");", dataType = "StoreTypeEnum")
    private StoreType storeType;

    /**
    *门店视频
    */
    @ApiModelProperty(notes = "门店视频", dataType = "String")
    private String videoUrl;

    /**
    *直播开始时间
    */
    @ApiModelProperty(notes = "直播开始时间", dataType = "String")
    private String beginTime;

    /**
    *直播结束时间
    */
    @ApiModelProperty(notes = "直播结束时间", dataType = "String")
    private String endTime;

    /**
    *录播地址
    */
    @ApiModelProperty(notes = "录播地址", dataType = "String")
    private String tapeUrl;

    @ApiModelProperty(notes="距离用户多远",dataType="String")
    private String distance;

    @ApiModelProperty(notes="门店位置信息",dataType="StorePosition")
    private StorePosition storePosition;

}
