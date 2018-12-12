package com.lhiot.oc.order.service;

import com.leon.microx.id.Generator;
import com.leon.microx.openfeign.CustomFeignException;
import com.leon.microx.util.*;
import com.leon.microx.web.result.Pages;
import com.leon.microx.web.result.Tips;
import com.lhiot.oc.order.entity.BaseOrder;
import com.lhiot.oc.order.entity.OrderProduct;
import com.lhiot.oc.order.entity.OrderStore;
import com.lhiot.oc.order.entity.type.OrderStatus;
import com.lhiot.oc.order.entity.type.ReceivingWay;
import com.lhiot.oc.order.entity.type.RefundStatus;
import com.lhiot.oc.order.feign.*;
import com.lhiot.oc.order.mapper.BaseOrderMapper;
import com.lhiot.oc.order.mapper.OrderFlowMapper;
import com.lhiot.oc.order.mapper.OrderProductMapper;
import com.lhiot.oc.order.mapper.OrderStoreMapper;
import com.lhiot.oc.order.model.*;
import com.lhiot.oc.order.model.type.ApplicationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.*;

import static com.lhiot.oc.order.entity.type.OrderStatus.WAIT_SEND_OUT;

/**
 * @author zhangfeng created in 2018/9/19 9:19
 **/
@Service
@Slf4j
@Transactional
public class OrderService {

    private BaseOrderMapper baseOrderMapper;
    private OrderProductMapper orderProductMapper;
    private OrderStoreMapper orderStoreMapper;
    private OrderFlowMapper orderFlowMapper;
    private Generator<Long> generator;
    private HaiDingService haiDingService;
    private PaymentService paymentService;
    private DeliverService deliverService;

    public OrderService(BaseOrderMapper baseOrderMapper, OrderProductMapper orderProductMapper, OrderStoreMapper orderStoreMapper, OrderFlowMapper orderFlowMapper, Generator<Long> generator, HaiDingService haiDingService, PaymentService paymentService, DeliverService deliverService) {
        this.baseOrderMapper = baseOrderMapper;
        this.orderProductMapper = orderProductMapper;
        this.orderStoreMapper = orderStoreMapper;
        this.orderFlowMapper = orderFlowMapper;
        this.generator = generator;
        this.haiDingService = haiDingService;
        this.paymentService = paymentService;
        this.deliverService = deliverService;
    }

    /**
     * 验证创建订单数据 可以是套餐 也可以是商品
     *
     * @param param 创建订单参数
     * @return Tips
     */
    public Tips validationParam(CreateOrderParam param) {
        //应付金额为空或者小于零
        if (param.getAmountPayable() < 0) {
            return Tips.warn("应付金额为空或者小于零");
        }
        if (param.getCouponAmount() > param.getTotalAmount()) {
            return Tips.warn("优惠金额不能大于订单总金额");
        }
        //不算优惠商品应付金额
        int productPayable = param.getAmountPayable() + param.getCouponAmount();
        //商品为空
        List<OrderProduct> orderProducts = param.getOrderProducts();
        if (CollectionUtils.isEmpty(orderProducts)) {
            return Tips.warn("商品为空");
        }
        int productAmount = 0;
        //校验商品数量
        for (OrderProduct orderProduct : orderProducts) {
            //判断传入购买份数
            if (Objects.isNull(orderProduct.getProductQty()) || orderProduct.getProductQty() <= 0) {
                return Tips.warn("商品购买数量为0");
            }
            productAmount += orderProduct.getTotalPrice();
        }
        if (!Objects.equals(productPayable, productAmount) || !Objects.equals(param.getTotalAmount(), productAmount)) {
            return Tips.warn("订单传入的金额有误");
        }
        //送货上门的订单，地址不能为空
        if (ReceivingWay.TO_THE_HOME.equals(param.getReceivingWay()) && Objects.isNull(param.getAddress())) {
            return Tips.warn("送货上门，地址为空");
        }
        return Tips.info("校验成功");
    }

    /**
     * 添加订单信息
     *
     * @param param       CreateOrderParam
     * @param orderStatus 插入时，订单的状态（已支付 WAIT_SEND_OUT,未支付 WAIT_PAYMENT）
     * @return OrderDetailResult
     */
    public OrderDetailResult createOrder(CreateOrderParam param, OrderStatus orderStatus) {
        BaseOrder baseOrder = param.toOrderObject();
        String orderCode = generator.get(0, ApplicationType.ref(param.getApplicationType()));
        baseOrder.setCode(orderCode);
        baseOrder.setHdOrderCode(orderCode);
        baseOrder.setStatus(orderStatus);
        baseOrderMapper.insert(baseOrder);

        List<OrderProduct> productList = param.getOrderProducts();
        for (OrderProduct orderProduct : productList) {
            orderProduct.setOrderId(baseOrder.getId());
            orderProduct.setRefundStatus(RefundStatus.NOT_REFUND);
        }
        orderProductMapper.batchInsert(param.getOrderProducts());

        OrderStore orderStore = param.getOrderStore();
        orderStore.setHdOrderCode(orderCode);
        orderStore.setOrderId(baseOrder.getId());
        orderStoreMapper.insert(orderStore);

        OrderDetailResult orderDetail = new OrderDetailResult();
        BeanUtils.of(orderDetail).populate(baseOrder);
        orderDetail.setOrderProductList(productList);
        orderDetail.setOrderStore(orderStore);
        return orderDetail;
    }

    /**
     * 支付回调修改订单状态，修改支付记录
     *
     * @param orderCode 订单编号
     * @param paidModel     支付信息
     */
    public void updateWaitPaymentToWaitSendOut(String orderCode, PaidModel paidModel) {
        int count = baseOrderMapper.updateStatusByCode(Maps.of("modifyStatus", OrderStatus.WAIT_SEND_OUT,
                "nowStatus", OrderStatus.WAIT_PAYMENT, "orderCode", orderCode, "payId", paidModel.getPayId()));
        if (count == 1) {
            //修改支付日志
            ResponseEntity response = paymentService.updatePaymentLog(paidModel.getPayId(), paidModel);
            if (response.getStatusCode().isError()) {
                throw new CustomFeignException(response);
            }
        }
    }

    /**
     * 发送海鼎修改订单状态
     *
     * @param orderCode 订单编号
     */
    public void updateWaitSendOutToSendOuting(String orderCode) {
        int count = baseOrderMapper.updateStatusByCode(Maps.of("modifyStatus", OrderStatus.SEND_OUTING,
                "nowStatus", OrderStatus.WAIT_SEND_OUT, "orderCode", orderCode));
        if (count == 1) {
            OrderDetailResult order = this.findByCode(orderCode, true, false);
            if (Objects.isNull(order)) {
                throw new RuntimeException("未查询到订单信息");
            }
            Store store = new Store();
            store.setId(order.getOrderStore().getStoreId());
            store.setCode(order.getOrderStore().getStoreCode());
            store.setName(order.getOrderStore().getStoreName());
            Tips tips = this.hdReduce(order, store, order.getHdOrderCode());
            if (tips.err()) {
                throw new RuntimeException(tips.getMessage());
            }
        }

    }

    /**
     * 海鼎备货，送货上门发送配送
     *
     * @param order 订单信息
     * @param param 配送信息
     */
    public void sendDelivery(OrderDetailResult order, DeliverParam param) {
        int count = baseOrderMapper.updateStatusByCode(Maps.of("modifyStatus", OrderStatus.WAIT_DISPATCHING, "nowStatus"
                , OrderStatus.SEND_OUTING, "orderCode", order.getCode(), "hdStockAt", Date.from(Instant.now())));
        if (count == 1) {
            DeliverOrder deliverOrder = this.convert(order, param);
            ResponseEntity response = deliverService.create(param.getDeliveryType(), param.getCoordinate(), deliverOrder);
            if (response.getStatusCode().isError()) {
                throw new CustomFeignException(response);
            }
        }
    }

    private DeliverOrder convert(OrderDetailResult order, DeliverParam param) {
        DeliverOrder deliverOrder = new DeliverOrder();
        deliverOrder.setAddress(order.getAddress());
        deliverOrder.setAmountPayable(order.getAmountPayable());
        deliverOrder.setApplyType(param.getApplicationType());
        deliverOrder.setBackUrl(param.getBackUrl());//配置回调
        deliverOrder.setContactPhone(order.getContactPhone());
        deliverOrder.setCouponAmount(order.getCouponAmount());


        deliverOrder.setCreateAt(Date.from(Instant.now()));

        deliverOrder.setDeliverTime(param.getDeliverTime());
        deliverOrder.setDeliveryFee(order.getDeliveryAmount());
        deliverOrder.setHdOrderCode(order.getHdOrderCode());
        deliverOrder.setLat(param.getLat());
        deliverOrder.setLng(param.getLng());
        deliverOrder.setOrderId(order.getId());
        deliverOrder.setOrderCode(order.getCode());
        deliverOrder.setReceiveUser(order.getReceiveUser());
        deliverOrder.setRemark(order.getRemark());
        deliverOrder.setStoreCode(order.getOrderStore().getStoreCode());
        deliverOrder.setStoreName(order.getOrderStore().getStoreName());
        deliverOrder.setTotalAmount(order.getTotalAmount());
        deliverOrder.setUserId(order.getUserId());

        List<DeliverProduct> deliverProductList = new ArrayList<>(order.getOrderProductList().size());
        order.getOrderProductList().forEach(item -> {
            DeliverProduct deliverProduct = new DeliverProduct();
            deliverProduct.setBarcode(item.getBarcode());
            deliverProduct.setBaseWeight(item.getTotalWeight().doubleValue());
            deliverProduct.setDeliverBaseOrderId(order.getId());
            deliverProduct.setDiscountPrice(item.getDiscountPrice());
            deliverProduct.setImage(item.getImage());
            deliverProduct.setLargeImage(item.getImage());
            deliverProduct.setPrice(item.getTotalPrice());
            deliverProduct.setProductName(item.getProductName());
            deliverProduct.setProductQty(item.getProductQty());
            deliverProduct.setSmallImage(item.getImage());
            deliverProduct.setStandardPrice((int) Calculator.div(item.getTotalPrice(), item.getProductQty()));
            deliverProduct.setStandardQty(Double.valueOf(item.getShelfQty().toString()));
            deliverProductList.add(deliverProduct);
        });
        deliverOrder.setDeliverOrderProductList(deliverProductList);//填充订单商品
        return deliverOrder;
    }

    /**
     * 门店调货
     *
     * @param targetStore   Store
     * @param operationUser String
     * @param orderId       Long
     * @return int
     */
    public void changeStore(Store targetStore, String operationUser, Long orderId, String hdOrderCode) {
        BaseOrder baseOrder = new BaseOrder();
        baseOrder.setId(orderId);
        baseOrder.setHdOrderCode(hdOrderCode);
        baseOrderMapper.updateHdOrderCodeById(baseOrder);

        //设置调货订单门店信息
        OrderStore orderStore = new OrderStore();
        orderStore.setHdOrderCode(hdOrderCode);
        orderStore.setOrderId(orderId);
        orderStore.setStoreId(targetStore.getId());
        orderStore.setStoreName(targetStore.getName());
        orderStore.setStoreCode(targetStore.getCode());
        orderStore.setOperationUser(operationUser);
        orderStore.setCreateAt(Date.from(Instant.now()));
        orderStoreMapper.insert(orderStore);
    }

    /**
     * 根据订单code修改订单状态（DISPATCHING，RECEIVED）
     *
     * @param orderCode    订单编号
     * @param modifyStatus 订单修改后的状态
     * @param nowStatus    订单当前状态
     * @return Tips
     */
    public Tips updateStatus(String orderCode, OrderStatus nowStatus, OrderStatus modifyStatus) {
        Map<String, Object> map = new HashMap<>();
        map.put("modifyStatus", modifyStatus);
        map.put("orderCode", orderCode);
        switch (modifyStatus) {
            case DISPATCHING:
                map.put("nowStatus", OrderStatus.SEND_OUTING);
                break;
            case RECEIVED:
                map.put("nowStatus", null);
                if (Objects.equals(nowStatus, WAIT_SEND_OUT)) {
                    map.put("hdStockAt", Date.from(Instant.now()));
                }
                break;
            default:
                return Tips.warn(nowStatus.getDescription() + "状态不可直接修改为" + modifyStatus.getDescription() + "状态");
        }
        int count = baseOrderMapper.updateStatusByCode(map);
        if (count == 1) {
            return Tips.empty();
        }
        return Tips.warn("修改状态失败");
    }

    public Tips hdReduce(OrderDetailResult order, Store store, String hdOrderCode) {
        HaiDingOrderParam haiDingOrderParam = new HaiDingOrderParam();
        BeanUtils.of(haiDingOrderParam).populate(order);
        haiDingOrderParam.setStoreName(store.getName());
        haiDingOrderParam.setStoreCode(store.getCode());
        haiDingOrderParam.setStoreId(store.getId());
        haiDingOrderParam.setHdOrderCode(hdOrderCode);

        //海鼎减库存失败重试机制
        Retry retry = Retry.of(() -> haiDingService.reduce(haiDingOrderParam)).count(3).intervalMs(30).run();
        ResponseEntity response = (ResponseEntity) retry.result();

        if (Objects.nonNull(retry.exception())) {
            Throwable error = Exceptions.unwrap(retry.exception());
            log.error(error.getMessage(), error);
            return Tips.warn("海鼎发送失败");
        }
        if (response.getStatusCode().isError()) {
            return Tips.warn("海鼎发送失败");
        }
        return Tips.empty();
    }

    /**
     * @param code              订单code
     * @param needProductList   是否查询商品信息
     * @param needOrderFlowList 是否查询订单状态流水
     * @return OrderDetailResult
     */
    @Nullable
    public OrderDetailResult findByCode(String code, boolean needProductList, boolean needOrderFlowList) {
        OrderDetailResult searchBaseOrderInfo = this.baseOrderMapper.selectByCode(code);
        if (Objects.isNull(searchBaseOrderInfo)) {
            return null;
        }
        this.addAttachments(searchBaseOrderInfo, needProductList, needOrderFlowList);
        return searchBaseOrderInfo;
    }

    /**
     * 默认不查询订单商品和流水信息
     *
     * @param code 订单code
     * @return OrderDetailResult
     */
    @Nullable
    public OrderDetailResult findByCode(String code) {
        return this.findByCode(code, false, false);
    }

    @Nullable
    public OrderDetailResult findById(Long id, boolean needProductList, boolean needOrderFlowList) {
        OrderDetailResult searchBaseOrderInfo = this.baseOrderMapper.selectById(id);
        if (Objects.isNull(searchBaseOrderInfo)) {
            return null;
        }
        this.addAttachments(searchBaseOrderInfo, needProductList, needOrderFlowList);
        return searchBaseOrderInfo;
    }

    @Nullable
    public OrderDetailResult findById(Long id) {
        return this.findById(id, false, false);
    }

    private void addAttachments(OrderDetailResult searchBaseOrderInfo, boolean needProductList, boolean needOrderFlowList) {
        searchBaseOrderInfo.setOrderStore(orderStoreMapper.findByHdOrderCode(searchBaseOrderInfo.getHdOrderCode()));
        if (needProductList) {
            searchBaseOrderInfo.setOrderProductList(orderProductMapper.selectOrderProductsByOrderId(searchBaseOrderInfo.getId()));
        }
        if (needOrderFlowList) {
            searchBaseOrderInfo.setOrderFlowList(orderFlowMapper.selectFlowByOrderId(searchBaseOrderInfo.getId()));
        }
    }

    /**
     * 查询订单列表
     *
     * @param param 参数
     * @return 分页订单数据
     */
    public Pages<OrderDetailResult> findList(BaseOrderParam param) {
        List<OrderDetailResult> list = baseOrderMapper.findList(param);
        boolean pageFlag = Objects.nonNull(param.getPage()) && Objects.nonNull(param.getRows()) && param.getPage() > 0 && param.getRows() > 0;
        int total = pageFlag ? baseOrderMapper.findCount(param) : list.size();
        return Pages.of(total, list);
    }

}
