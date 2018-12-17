package com.lhiot.oc.delivery.model;

import com.lhiot.oc.delivery.entity.DeliverAtType;
import com.lhiot.oc.delivery.entity.DeliverFeeRuleDetail;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author zhangfeng create in 12:04 2018/12/11
 */
@Data
public class DeliverFeeRuleParam {
    @Min(1)
    private Long id;

    @Min(0)
    private Integer minOrderAmount;

    @Min(1)
    private Integer maxOrderAmount;

    @NotNull
    private DeliverAtType deliveryAtType;

    private String createBy;

    private List<String> deleteIds;

    private List<DeliverFeeRuleDetail> detailList;

}
