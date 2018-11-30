package com.lhiot.oc.order.service;

import com.leon.microx.id.Generator;
import com.leon.microx.id.Snowflake;
import com.leon.microx.util.*;
import com.leon.microx.web.result.Tips;
import com.lhiot.dc.dictionary.DictionaryClient;
import com.lhiot.oc.order.entity.BaseOrder;
import com.lhiot.oc.order.entity.OrderProduct;
import com.lhiot.oc.order.entity.OrderRefund;
import com.lhiot.oc.order.entity.OrderStore;
import com.lhiot.oc.order.entity.type.*;
import com.lhiot.oc.order.event.OrderFlowEvent;
import com.lhiot.oc.order.feign.HaiDingService;
import com.lhiot.oc.order.mapper.*;
import com.lhiot.oc.order.model.*;
import com.lhiot.oc.order.model.type.ApplicationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

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
    private OrderRefundMapper orderRefundMapper;
    private ApplicationEventPublisher publisher;
    private DictionaryClient client;
    private HaiDingService haiDingService;

    public OrderService(BaseOrderMapper baseOrderMapper, OrderProductMapper orderProductMapper, OrderStoreMapper orderStoreMapper, OrderFlowMapper orderFlowMapper, Generator<Long> generator, OrderRefundMapper orderRefundMapper, ApplicationEventPublisher publisher, DictionaryClient client, HaiDingService haiDingService) {
        this.baseOrderMapper = baseOrderMapper;
        this.orderProductMapper = orderProductMapper;
        this.orderStoreMapper = orderStoreMapper;
        this.orderFlowMapper = orderFlowMapper;
        this.generator = generator;
        this.orderRefundMapper = orderRefundMapper;
        this.publisher = publisher;
        this.client = client;
        this.haiDingService = haiDingService;
    }

    /**
     * 添加订单信息
     *
     * @param param CreateOrderParam
     * @return OrderDetailResult
     */
    public OrderDetailResult createOrder(CreateOrderParam param) {
        BaseOrder baseOrder = param.toOrderObject();
        String orderCode = generator.get(0, ApplicationType.ref(param.getApplicationType()));
        baseOrder.setCode(orderCode);
        baseOrder.setHdOrderCode(orderCode);
        baseOrder.setHdStatus(HdStatus.NOT_SEND);
        baseOrder.setStatus(OrderStatus.WAIT_PAYMENT);
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
     * 依据订单编码退货记录退货日志以及修改订单状态
     *
     * @param pair             订单修改后状态以及退货日志状态
     * @param returnOrderParam 退货信息
     * @param order            订单信息
     */
    public void refundUpdateByCode(Pair<OrderRefundStatus, OrderStatus> pair, ReturnOrderParam returnOrderParam, OrderDetailResult order) {
        BaseOrder baseOrder = new BaseOrder();
        baseOrder.setCode(order.getCode());
        baseOrder.setId(order.getId());
        baseOrder.setStatus(pair.getSecond());

        OrderRefund orderRefund = new OrderRefund();
        BeanUtils.of(orderRefund).populate(returnOrderParam);
        orderRefund.setHdOrderCode(order.getHdOrderCode());
        orderRefund.setOrderId(order.getId());
        orderRefund.setUserId(order.getUserId());
        orderRefund.setRefundStatus(pair.getFirst());
        orderRefund.setApplyAt(Date.from(Instant.now()));
        if (Objects.equals(OrderRefundStatus.ALREADY_RETURN,pair.getFirst())){
            orderRefund.setDisposeAt(Date.from(Instant.now()));
        }

        int result = this.baseOrderMapper.updateOrderStatusByCode(baseOrder);
        if (result > 0) {
            //记录退货表
            orderRefundMapper.insert(orderRefund);
            //写退款订单商品标识
            List<String> orderProductIds = Arrays.asList(StringUtils.tokenizeToStringArray(orderRefund.getOrderProductIds(), ","));
            orderProductMapper.updateOrderProductByIds(Maps.of("orderId", baseOrder.getId(),
                    "refundStatus", RefundStatus.REFUND,
                    "orderProductIds", orderProductIds));
        }
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
     * 验证订单状态修改
     *
     * @param modifyStatus 修改后订单状态
     * @param nowStatus    订单当前状态
     * @return Tips
     */
    public Tips validateUpdateStatus(OrderStatus modifyStatus, OrderStatus nowStatus) {
        Tips<BaseOrder> tips = Tips.empty();
        BaseOrder order = new BaseOrder();
        order.setStatus(modifyStatus);
        switch (modifyStatus) {
            case FAILURE:
                if (!Objects.equals(nowStatus, OrderStatus.WAIT_PAYMENT)) {
                    return Tips.warn(nowStatus.getDescription() + "状态不可取消订单");
                }
                break;
            case SEND_OUT:
                if (!Objects.equals(nowStatus, OrderStatus.WAIT_SEND_OUT)) {
                    return Tips.warn(nowStatus.getDescription() + "状态不可进行发货");
                }
                order.setHdStockAt(Date.from(Instant.now()));
                break;
            case DISPATCHING:
                if (!Objects.equals(nowStatus, OrderStatus.SEND_OUT)) {
                    return Tips.warn(nowStatus.getDescription() + "状态不可进行配送");
                }
                break;
            case RECEIVED:
                if (!Objects.equals(nowStatus, WAIT_SEND_OUT) &&
                        !Objects.equals(nowStatus, OrderStatus.DISPATCHING)) {
                    return Tips.warn(nowStatus.getDescription() + "状态不可更改为已收货");
                }
                if (Objects.equals(nowStatus, WAIT_SEND_OUT)){
                    order.setHdStockAt(Date.from(Instant.now()));
                }
                break;
            case WAIT_SEND_OUT:
                order.setHdStatus(HdStatus.SEND_OUT);
                break;
            case RETURNING:
            case ALREADY_RETURN:
                return Tips.warn(nowStatus.getDescription() + "状态不可直接修改为" + modifyStatus.getDescription() + "状态");
            default:
                break;
        }
        tips.setData(order);
        return tips;
    }

    /**
     * 根据订单code修改订单状态
     *
     * @param orderStatus       订单修改后的状态
     * @param orderDetailResult 订单信息
     * @return Tips
     */
    public Tips updateStatus(OrderDetailResult orderDetailResult, OrderStatus orderStatus) {
        Tips tips = this.validateUpdateStatus(orderStatus, orderDetailResult.getStatus());
        if (tips.err()) {
            return tips;
        }
        BaseOrder baseOrder = (BaseOrder) tips.getData();
        baseOrder.setCode(orderDetailResult.getCode());
        int result = baseOrderMapper.updateOrderStatusByCode(baseOrder);
        if (result > 0) {
            publisher.publishEvent(new OrderFlowEvent(orderDetailResult.getStatus(), orderStatus, orderDetailResult.getId()));
            return Tips.empty();
        }
        return Tips.warn("修改状态失败");
    }

    /**
     * 订单退货验证
     *
     * @param orderCode 订单编号
     * @param param     退货信息
     * @return Tips
     */
    public Tips validateRefund(String orderCode, ReturnOrderParam param) {

        if (Objects.equals(RefundType.PART, param.getRefundType()) && StringUtils.isBlank(param.getOrderProductIds())) {
            return Tips.warn("部分退货，商品不可为空！");
        }
        OrderDetailResult order = this.findByCode(orderCode);
        if (Objects.isNull(order)) {
            return Tips.warn("订单不存在！");
        }
        if (Objects.equals(order.getAllowRefund(), AllowRefund.NO)) {
            return Tips.warn("该订单不可以退货！");
        }
        //只允许待发货 已发货 退货中的订单退货
        if (!Objects.equals(order.getStatus(), WAIT_SEND_OUT) &&
                !Objects.equals(order.getStatus(), OrderStatus.SEND_OUT) &&
                !Objects.equals(order.getStatus(), OrderStatus.RECEIVED)) {
            return Tips.warn("只允许待发货/已发货的订单退货，当前订单状态为:" + order.getStatus().getDescription());
        }
        return Tips.empty().data(order);
    }

    /**
     * 处理确认退货，修改退货状态
     * @param orderId 订单Id
     * @param orderCode 订单编号
     * @return boolean
     */
    public boolean disposeRefund(Long orderId,String orderCode){
        int count = baseOrderMapper.updateStatusByDisposeRefund(Maps.of("code", orderCode, "status", OrderStatus.ALREADY_RETURN));
        if (count >0){
            orderRefundMapper.updateByOrderId(Maps.of("refundStatus",OrderRefundStatus.ALREADY_RETURN,"orderId",orderId));
            return true;
        }
        return false;
    }

    /**
     * 海鼎取消订单
     *
     * @param hdOrderCode 海鼎订单编号
     * @param reason      退货原因
     * @return Pair
     */
    public Pair<OrderRefundStatus, OrderStatus> hdCancel(String hdOrderCode, String reason) {
        ResponseEntity cancelResponse = haiDingService.hdCancel(hdOrderCode, reason);
        if (Objects.isNull(cancelResponse) || cancelResponse.getStatusCode().isError()) {
            return Pair.empty();
        }
        return Pair.of(OrderRefundStatus.ALREADY_RETURN, OrderStatus.ALREADY_RETURN);
    }

    /**
     * 海鼎退货退款
     *
     * @param order       订单信息
     * @param refundParam 退货信息
     * @return Pair
     */
    public Pair<OrderRefundStatus, OrderStatus> hdRefund(OrderDetailResult order, ReturnOrderParam refundParam) {
        HaiDingOrderParam haiDingOrderParam = new HaiDingOrderParam();
        BeanUtils.of(haiDingOrderParam).populate(order);
        List<OrderProduct> refundProducts = orderProductMapper.selectOrderProductsByIds(Arrays.asList(StringUtils.tokenizeToStringArray(refundParam.getOrderProductIds(), ",")));
        if (CollectionUtils.isEmpty(refundProducts)) {
            return Pair.empty();
        }
        OrderStore store = order.getOrderStore();
        haiDingOrderParam.setStoreName(store.getStoreName());
        haiDingOrderParam.setStoreCode(store.getStoreCode());
        haiDingOrderParam.setStoreId(store.getStoreId());
        haiDingOrderParam.setReturnReason(refundParam.getReason());
        haiDingOrderParam.setOrderProducts(refundProducts);
        ResponseEntity refundResponse = haiDingService.hdRefund(haiDingOrderParam);
        if (Objects.isNull(refundResponse) || refundResponse.getStatusCode().isError()) {
            return Pair.empty();
        }
        return Pair.of(OrderRefundStatus.RETURNING, OrderStatus.RETURNING);
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

        if (response.getStatusCode().isError()) {
            return Tips.warn("海鼎发送失败");
        }
        if (Objects.nonNull(retry.exception())) {
            Throwable error = Exceptions.unwrap(retry.exception());
            log.error(error.getMessage(), error);
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
}
