package com.lhiot.oc.order.model;

import com.leon.microx.util.BeanUtils;
import com.lhiot.dc.dictionary.DictionaryClient;
import com.lhiot.dc.dictionary.module.Dictionary;
import com.lhiot.oc.order.entity.BaseOrder;
import com.lhiot.oc.order.entity.OrderProduct;
import com.lhiot.oc.order.entity.OrderStore;
import com.lhiot.oc.order.entity.type.ReceivingWay;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Data
@ApiModel
public class CreateOrderParam {

    @ApiModelProperty(notes = "业务用户Id", dataType = "Long")
    private Long userId;
    @ApiModelProperty(notes = "应用类型", dataType = "Long")
    private String applicationType;
    private String orderType;
    private ReceivingWay receivingWay;
    private Integer couponAmount = 0;
    private Integer totalAmount;
    @ApiModelProperty(notes = "配送费", dataType = "Integer")
    private Integer deliveryAmount = 0;
    private Integer amountPayable = 0;
    @ApiModelProperty(notes = "收货地址：门店自提订单填写收货地址", dataType = "String")
    private String address;
    @ApiModelProperty(notes = "收货人", dataType = "String")
    private String receiveUser;
    @ApiModelProperty(notes = "收货人昵称", dataType = "String")
    private String nickname;
    @ApiModelProperty(notes = "收货人联系方式", dataType = "String")
    private String contactPhone;
    private String remark;
    @ApiModelProperty(notes = "提货截止时间", dataType = "String")
    private Date deliveryEndAt;
    @ApiModelProperty(notes = "配送时间 json格式如 {\"display\":\"立即配送\",\"startTime\":\"2018-08-15 11:30:00\",\"endTime\":\"2018-08-15 12:30:00\"}", dataType = "String")
    private String deliveryAt;
    @ApiModelProperty(notes = "商品列表", dataType = "OrderProduct")
    private List<OrderProduct> orderProducts;
    @ApiModelProperty(notes = "门店信息", dataType = "OrderStoreParam")
    private OrderStore orderStore;

    /**
     * 对象属性复制
     *
     * @return
     */
    public BaseOrder toOrderObject() {
        BaseOrder baseOrder = new BaseOrder();
        this.applicationType = this.applicationType.toUpperCase();
        this.orderType = this.orderType.toUpperCase();
        BeanUtils.of(baseOrder).populate(BeanUtils.of(this).toMap());
        return baseOrder;
    }

    /**
     * 验证入参数据字典
     *
     * @return Tips
     */
    public boolean validateDictionary(DictionaryClient client) {
        boolean hasApplicationType = client.dictionary("applications")
                .map(dictionary -> dictionary.hasEntry(this.applicationType))
                .orElse(false);

        boolean hasOrderType = client.dictionary("orderTypes")
                .map(dictionary -> dictionary.hasEntry(this.orderType))
                .orElse(false);

        return hasApplicationType && hasOrderType;
    }
}
