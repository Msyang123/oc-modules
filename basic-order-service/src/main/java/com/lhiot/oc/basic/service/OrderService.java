package com.lhiot.oc.basic.service;

import com.leon.microx.support.result.Tips;
import com.leon.microx.util.BeanUtils;
import com.leon.microx.util.Maps;
import com.leon.microx.util.SnowflakeId;
import com.leon.microx.util.StringUtils;
import com.lhiot.oc.basic.mapper.BaseOrderMapper;
import com.lhiot.oc.basic.mapper.OrderProductMapper;
import com.lhiot.oc.basic.mapper.OrderStoreMapper;
import com.lhiot.oc.basic.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
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
    private OrderProductService orderProductService;
    private OrderProductMapper orderProductMapper;
    private OrderStoreMapper orderStoreMapper;
    private OrderFlowService orderFlowService;
    private SnowflakeId snowflakeId;

    public OrderService(BaseOrderMapper baseOrderMapper, OrderProductService orderProductService, OrderProductMapper orderProductMapper, OrderStoreMapper orderStoreMapper, OrderFlowService orderFlowService, SnowflakeId snowflakeId) {
        this.baseOrderMapper = baseOrderMapper;
        this.orderProductService = orderProductService;
        this.orderProductMapper = orderProductMapper;
        this.orderStoreMapper = orderStoreMapper;
        this.orderFlowService = orderFlowService;
        this.snowflakeId = snowflakeId;
    }

    /**
     * 添加订单信息
     *
     * @param param
     * @return
     */
    public OrderDetailResult createOrder(CreateOrderParam param) {
        BaseOrder baseOrder = param.toOrderObject();
        String orderCode = snowflakeId.stringId();
        baseOrder.setCode(orderCode);
        baseOrder.setHdOrderCode(orderCode);
        baseOrderMapper.insert(baseOrder);

        List<OrderProduct> productList = param.getOrderProducts();
        for (OrderProduct orderProduct : productList) {
            orderProduct.setOrderId(baseOrder.getId());
        }
        orderProductMapper.batchInsert(param.getOrderProducts());
        OrderStore orderStore = param.getOrderStore();
        orderStore.setHdOrderCode(orderCode);
        orderStore.setOrderId(baseOrder.getId());
        orderStoreMapper.insert(orderStore);

        //构建写order_flow库的数据
        orderFlowService.create(null,baseOrderInfo);

        OrderDetailResult orderDetail = new OrderDetailResult();
        BeanUtils.of(orderDetail).populate(baseOrder);
        orderDetail.setOrderProductList(productList);
        orderDetail.setOrderStore(orderStore);
        return orderDetail;
    }


    /**
     * 验证创建订单数据 可以是套餐 也可以是商品
     *
     * @param param 创建订单参数
     * @return
     */
    public Tips validationParam(CreateOrderParam param) {
        //应付金额为空或者小于零
        if (param.getAmountPayable() < 0) {
            return Tips.of(-1, "应付金额为空或者小于零");
        }
        if (param.getCouponAmount() > param.getTotalAmount()) {
            return Tips.of(-1, "优惠金额不能大于订单总金额");
        }
        //不算优惠商品应付金额
        int productPayable = param.getAmountPayable() + param.getCouponAmount();
        //商品为空
        List<OrderProduct> orderProducts = param.getOrderProducts();
        if (CollectionUtils.isEmpty(orderProducts)) {
            return Tips.of(-1, "商品为空");
        }
        int productAmount = 0;
        //校验商品数量
        for (OrderProduct orderProduct : orderProducts) {
            //判断传入购买份数
            if (Objects.isNull(orderProduct.getProductQty()) || orderProduct.getProductQty() <= 0) {
                return Tips.of(-1, "商品购买数量为0");
            }
            productAmount += orderProduct.getTotalPrice();
        }
        if (!Objects.equals(productPayable, productAmount) || !Objects.equals(param.getTotalAmount(), productAmount)) {
            return Tips.of(-1, "订单传入的金额有误");
        }
        //送货上门的订单，地址不能为空
        if (ReceivingWay.TO_THE_HOME.equals(param.getReceivingWay()) && Objects.isNull(param.getAddress())) {
            return Tips.of(-1, "送货上门，地址为空");
        }
        return Tips.of(1, "校验成功");
    }

    /**
     * 依据id修改订单状态
     *
     * @param baseOrderInfo
     * @return
     */
    public int updateOrderStatusById(BaseOrderInfo baseOrderInfo) {
        BaseOrderInfo searchBaseOrderInfo = this.baseOrderMapper.findById(baseOrderInfo.getId());
        if (Objects.isNull(searchBaseOrderInfo)) {
            return 0;
        }
        int result = this.baseOrderMapper.updateOrderStatusById(baseOrderInfo);
        if (result > 0) {
            //构建写order_flow库的数据
            orderFlowService.create(searchBaseOrderInfo, baseOrderInfo);
        }
        return result;
    }

    public int updateOrderStatusByCode(BaseOrderInfo baseOrderInfo) {
        BaseOrderInfo searchBaseOrderInfo = this.baseOrderMapper.findById(baseOrderInfo.getId());
        if (Objects.isNull(searchBaseOrderInfo)) {
            return 0;
        }
        int result = this.baseOrderMapper.updateOrderStatusByCode(baseOrderInfo);
        if (result > 0) {
            //构建写order_flow库的数据
            orderFlowService.create(searchBaseOrderInfo, baseOrderInfo);
        }
        return result;
    }

    /**
     * 依据订单编码退货
     *
     * @param orderCode
     * @return
     */
    public int refundOrderByCode(String orderCode, ReturnOrderParam returnOrderParam) {
        BaseOrderInfo searchBaseOrderInfo = this.baseOrderMapper.findByCode(orderCode);
        if (Objects.isNull(searchBaseOrderInfo)) {
            return 0;
        }
        BaseOrderInfo baseOrderInfo = new BaseOrderInfo();
        baseOrderInfo.setCode(orderCode);
        baseOrderInfo.setStatus(OrderStatus.ALREADY_RETURN);
        //TODO 设置退款理由 baseOrderInfo.set
        int result = this.baseOrderMapper.updateOrderStatusByCode(baseOrderInfo);
        if (result > 0) {
            //写退款订单商品标识
            List<String> orderProductIds = Arrays.asList(StringUtils.split(returnOrderParam.getOrderProductIds(), ","));
            this.orderProductService.updateOrderProductByIds(searchBaseOrderInfo.getId(), RefundStatus.REFUND, orderProductIds);
            //构建写order_flow库的数据
            orderFlowService.create(searchBaseOrderInfo, baseOrderInfo);
        }
        return result;
    }

    /**
     * 依据订单编码退货(已收货发起退货申请)
     *
     * @param orderCode
     * @return
     */
    public int refundOrderApplyByCode(String orderCode, ReturnOrderParam returnOrderParam) {
        BaseOrderInfo searchBaseOrderInfo = this.baseOrderMapper.findByCode(orderCode);
        if (Objects.isNull(searchBaseOrderInfo)) {
            return 0;
        }
        BaseOrderInfo baseOrderInfo = new BaseOrderInfo();
        baseOrderInfo.setCode(orderCode);
        baseOrderInfo.setStatus(OrderStatus.RETURNING);//设置为退货中
        //TODO 设置退款理由 baseOrderInfo.set
        int result = this.baseOrderMapper.updateOrderStatusByCode(baseOrderInfo);
        if (result > 0) {
            //写退款订单商品标识
            List<String> orderProductIds = Arrays.asList(StringUtils.split(returnOrderParam.getOrderProductIds(), ","));
            this.orderProductService.updateOrderProductByIds(searchBaseOrderInfo.getId(), RefundStatus.REFUND, orderProductIds);
            //构建写order_flow库的数据
            orderFlowService.create(searchBaseOrderInfo, baseOrderInfo);
        }
        return result;
    }

    /**
     * 门店调货
     * @param targetStore
     * @param operationUser
     * @param orderId
     * @return
     */
    public int changeStore(Store targetStore,String operationUser,Long orderId){

        //修改订单为待收货状态
        BaseOrderInfo baseOrderInfo=new BaseOrderInfo();
        baseOrderInfo.setId(orderId);
        baseOrderInfo.setHdOrderCode(snowflakeId.stringId());
        int result = baseOrderMapper.updateHdOrderCodeById(baseOrderInfo);

        if(result>0){
            //设置调货订单门店信息
            OrderStore orderStore=new OrderStore();
            orderStore.setHdOrderCode(baseOrderInfo.getHdOrderCode());
            orderStore.setOrderId(orderId);
            orderStore.setStoreId(targetStore.getId());
            orderStore.setStoreName(targetStore.getStoreName());
            orderStore.setStoreCode(targetStore.getStoreCode());
            orderStore.setOperationUser(operationUser);
            orderStore.setCreateAt(new Date());
            orderStoreMapper.insert(orderStore);
        }
        return result;
    }


    @Nullable
    public BaseOrderInfo findByCode(String code, boolean needProductList, boolean needOrderFlowList) {
        BaseOrderInfo searchBaseOrderInfo = this.baseOrderMapper.findByCode(code);
        if (Objects.isNull(searchBaseOrderInfo)) {
            return null;
        }
        if (needProductList) {
            searchBaseOrderInfo.setOrderProductList(this.orderProductService.findOrderProductsByOrderId(searchBaseOrderInfo.getId()));
        }
        if (needOrderFlowList) {
            searchBaseOrderInfo.setOrderFlowList(this.orderFlowService.flowByOrderId(searchBaseOrderInfo.getId()));
        }
        return searchBaseOrderInfo;
    }

    @Nullable
    public BaseOrderInfo findByCode(String code) {
        return this.findByCode(code, false, false);
    }

    @Nullable
    public BaseOrderInfo findById(Long id, boolean needProductList, boolean needOrderFlowList) {
        BaseOrderInfo searchBaseOrderInfo = this.baseOrderMapper.findById(id);
        if (Objects.isNull(searchBaseOrderInfo)) {
            return null;
        }
        if (needProductList) {
            searchBaseOrderInfo.setOrderProductList(this.orderProductService.findOrderProductsByOrderId(searchBaseOrderInfo.getId()));
        }
        if (needOrderFlowList) {
            searchBaseOrderInfo.setOrderFlowList(this.orderFlowService.flowByOrderId(searchBaseOrderInfo.getId()));
        }
        return searchBaseOrderInfo;
    }

    @Nullable
    public BaseOrderInfo findById(Long id) {
        return this.findById(id, false, false);
    }

}
