package com.lhiot.oc.order.feign;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * @author zhangfeng create in 8:45 2018/12/5
 */
@Data
public class PaidModel {

    @ApiModelProperty(notes = "第三方支付产生的商户单号",dataType = "String")
    private String payId;

    @ApiModelProperty(notes = "银行类型",dataType = "String")
    private String bankType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(notes = "支付时间",dataType = "Date")
    private Date payAt;

    @ApiModelProperty(notes = "支付平台交易号",dataType = "String")
    private String tradeId;
}
