package com.lhiot.oc.delivery.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lhiot.oc.delivery.entity.DeliverAtType;
import lombok.Data;

import java.util.Objects;

/**
 * @author zhangfeng create in 8:56 2018/12/12
 */
@Data
public class DeliverFeeSearchParam {
    private Integer minOrderAmount;

    private Integer maxOrderAmount;

    private DeliverAtType deliveryAtType;

    private Integer rows;

    private Integer page;

    @JsonIgnore
    private Integer startRows;

    public Integer getStartRows(){
        if (Objects.nonNull(rows) && Objects.nonNull(page) && rows > 0 && page >0){
            return (page-1)*rows;
        }
        return null;
    }
}
