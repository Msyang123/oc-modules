package com.lhiot.oc.basic.domain;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.lhiot.order.domain.common.PagerRequestObject;
import com.lhiot.order.domain.enums.RefundStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
* 描述：订单套餐表
* @author yijun
* @date 2018-07-21
*/
@Data
@ToString
@ApiModel
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class OrderAssortment extends PagerRequestObject implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(notes = "id", dataType = "Long")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(notes = "套餐id", dataType = "Long")
    private Long assortmentId;

    @ApiModelProperty(notes = "购买数量", dataType = "Integer")
    private Integer buyCount;

    @ApiModelProperty(notes = "是否退货: REFUND-已退货 NOT_REFUND-未退货", dataType = "RefundStatus")
    private RefundStatus refundStatus;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(notes = "订单id", dataType = "Long")
    private Long orderId;

    @ApiModelProperty(notes = "购买价格", dataType = "Integer")
    private Integer price;

    @ApiModelProperty(notes = "实付金额", dataType = "Integer")
    private Integer amountPayable;

    @ApiModelProperty(notes = "套餐优惠金额", dataType = "Integer")
    private Integer assortmentDiscountPrice;
    
    @ApiModelProperty(notes = "套餐名称", dataType = "String")
    private String assortmentName;
    
    @ApiModelProperty(notes = "套餐图片", dataType = "String")
    private String assortmentImage;

    @ApiModelProperty(notes = "套餐商品", dataType = "List")
    private List<OrderProduct> orderProductList;
    
}
