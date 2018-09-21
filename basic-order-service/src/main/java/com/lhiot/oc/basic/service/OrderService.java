package com.lhiot.oc.basic.service;

import com.leon.microx.support.result.Tips;
import com.leon.microx.util.BeanUtils;
import com.leon.microx.util.SnowflakeId;
import com.lhiot.oc.basic.mapper.BaseOrderMapper;
import com.lhiot.oc.basic.mapper.OrderProductMapper;
import com.lhiot.oc.basic.mapper.OrderStoreMapper;
import com.lhiot.oc.basic.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

/**
 * @Author zhangfeng created in 2018/9/19 9:19
 **/
@Service
@Slf4j
@Transactional
public class OrderService {

    private BaseOrderMapper baseOrderMapper;
    private OrderProductMapper orderProductMapper;
    private OrderStoreMapper orderStoreMapper;
    private SnowflakeId snowflakeId;

    public OrderService(BaseOrderMapper baseOrderMapper, OrderProductMapper orderProductMapper, OrderStoreMapper orderStoreMapper, SnowflakeId snowflakeId) {
        this.baseOrderMapper = baseOrderMapper;
        this.orderProductMapper = orderProductMapper;
        this.orderStoreMapper = orderStoreMapper;
        this.snowflakeId = snowflakeId;
    }

    /**
     * 添加订单信息
     * @param   param
     * @param orderProducts
     * @param orderStore
     * @return
     */
    public OrderDetailResult createOrder(CreateOrderParam param,List<OrderProduct> orderProducts,OrderStore orderStore) {
        BaseOrderInfo baseOrderInfo =  param.toOrderObject();
        String orderCode = snowflakeId.stringId();
        baseOrderInfo.setCode(orderCode);
        baseOrderInfo.setHdOrderCode(orderCode);
        baseOrderInfo =  baseOrderMapper.insert(baseOrderInfo);

        for (OrderProduct orderProduct : orderProducts){
            orderProduct.setOrderId(baseOrderInfo.getId());
        }
        orderProductMapper.batchInsert(orderProducts);

        orderStore.setHdOrderCode(baseOrderInfo.getHdOrderCode());
        orderStore.setOrderId(baseOrderInfo.getId());
        orderStoreMapper.insert(orderStore);

        OrderDetailResult orderDetail = new OrderDetailResult();
        BeanUtils.of(orderDetail).populate(baseOrderInfo);
        orderDetail.setOrderProductList(orderProducts);
        orderDetail.setOrderStore(orderStore);
        return  orderDetail;
    }


    /**
     * 验证创建订单数据 可以是套餐 也可以是商品
     *
     * @param param 创建订单参数
     * @return
     */
    public Tips validationParam(CreateOrderParam param) {
        //应付金额为空或者小于零
        if (param.getAmountPayable() <= 0) {
            return Tips.of(-1, "应付金额为空或者小于零");
        }
        if (param.getCouponAmount() >= param.getTotalAmount()) {
            return Tips.of(-1, "优惠金额不能大于订单总金额");
        }
        //不算优惠商品应付金额
        int productPayable = param.getAmountPayable() + param.getCouponAmount()-param.getDeliveryAmount();
        //商品为空
        List<OrderProductParam> orderProducts = param.getOrderProducts();
        if (CollectionUtils.isEmpty(orderProducts)) {
            return Tips.of(-1, "商品为空");
        }
        int productAmount = 0;
        //校验商品数量
        for (OrderProductParam orderProductParam : orderProducts) {
            //判断传入购买份数
            if (Objects.isNull(orderProductParam.getBuyCount()) || orderProductParam.getBuyCount() <= 0) {
                return Tips.of(-1, "商品购买数量为0");
            }
            productAmount += orderProductParam.getPrice() * orderProductParam.getBuyCount();
        }
        if (!Objects.equals(productPayable, productAmount) || !Objects.equals(param.getTotalAmount(),productAmount)) {
            return Tips.of(-1, "订单传入的金额有误");
        }
        //送货上门的订单，地址不能为空
        if (ReceivingWay.TO_THE_HOME.equals(param.getReceivingWay()) && Objects.isNull(param.getAddress())) {
            return Tips.of(-1, "送货上门，地址为空");
        }
        return Tips.of(1, "校验成功");
    }

}
