package com.lhiot.oc.delivery.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lhiot.oc.delivery.entity.DeliverAtType;
import com.lhiot.oc.delivery.entity.DeliverFeeRuleDetail;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

/**
 * @author zhangfeng create in 12:04 2018/12/11
 */
@Data
public class DeliverFeeRulesResult {
    @Min(1)
    private Long id;

    @Min(0)
    private Integer minOrderAmount;

    @Min(1)
    private Integer maxOrderAmount;

    @NotNull
    private DeliverAtType deliveryAtType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateAt;

    private String createBy;

    private List<DeliverFeeRuleDetail> detailList;

}
