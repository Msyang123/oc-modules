package com.lhiot.oc.order.model.type;

import lombok.Getter;

/**
 * @author zhangfeng created in 2018/9/20 12:22
 **/
public enum AttachmentType {
    PRIMARY_IMAGE("商品主图"),
    SMALL_IMAGE("商品小图"),
    LARGE_IMAGE("商品大图"),
    VIDEO_IMAGE("视频图"),
    VIDEO("视频");
    @Getter
    private String description;

    AttachmentType(String description) {
        this.description = description;
    }


}
