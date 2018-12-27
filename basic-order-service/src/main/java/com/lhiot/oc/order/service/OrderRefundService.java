package com.lhiot.oc.order.service;

import com.leon.microx.exception.ServiceException;
import com.leon.microx.openfeign.CustomFeignException;
import com.leon.microx.util.BeanUtils;
import com.leon.microx.util.Maps;
import com.leon.microx.util.StringUtils;
import com.leon.microx.web.result.Tips;
import com.lhiot.oc.order.entity.OrderProduct;
import com.lhiot.oc.order.entity.OrderRefund;
import com.lhiot.oc.order.entity.OrderStore;
import com.lhiot.oc.order.entity.type.*;
import com.lhiot.oc.order.feign.HaiDingService;
import com.lhiot.oc.order.feign.PaymentService;
import com.lhiot.oc.order.feign.RefundParam;
import com.lhiot.oc.order.mapper.BaseOrderMapper;
import com.lhiot.oc.order.mapper.OrderProductMapper;
import com.lhiot.oc.order.mapper.OrderRefundMapper;
import com.lhiot.oc.order.model.HaiDingOrderParam;
import com.lhiot.oc.order.model.OrderDetailResult;
import com.lhiot.oc.order.model.ReturnOrderParam;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @author zhangfeng create in 12:02 2018/12/6
 */
@Service
@Transactional
public class OrderRefundService {

    private BaseOrderMapper baseOrderMapper;
    private PaymentService paymentService;
    private OrderProductMapper productMapper;
    private OrderRefundMapper refundMapper;
    private HaiDingService haiDingService;

    public OrderRefundService(BaseOrderMapper baseOrderMapper, PaymentService paymentService, OrderProductMapper productMapper, OrderRefundMapper refundMapper, HaiDingService haiDingService) {
        this.baseOrderMapper = baseOrderMapper;
        this.paymentService = paymentService;
        this.productMapper = productMapper;
        this.refundMapper = refundMapper;
        this.haiDingService = haiDingService;
    }

    public Tips validateRefund(String orderCode, RefundType refundType, String productIds) {
        OrderDetailResult order = baseOrderMapper.selectByCode(orderCode);
        if (Objects.isNull(order)) {
            return Tips.warn("订单不存在！");
        }
        if (!Objects.equals(OrderStatus.WAIT_SEND_OUT, order.getStatus()) && !Objects.equals(OrderStatus.SEND_OUTING, order.getStatus()) &&
                !Objects.equals(OrderStatus.WAIT_DISPATCHING, order.getStatus()) && !Objects.equals(OrderStatus.RECEIVED, order.getStatus())) {
            return Tips.warn("订单状态不可退款");
        }
        if (Objects.equals(RefundType.PART, refundType) && StringUtils.isBlank(productIds)) {
            return Tips.warn("部分退货，商品不可为空！");
        }
        if (Objects.equals(order.getAllowRefund(), AllowRefund.NO)) {
            return Tips.warn("该订单不可以退货！");
        }
        return Tips.empty();
    }

    private void insertRefundLog(OrderDetailResult order, ReturnOrderParam param) {
        OrderRefund orderRefund = new OrderRefund();
        BeanUtils.of(orderRefund).populate(param);
        orderRefund.setHdOrderCode(order.getHdOrderCode());
        orderRefund.setOrderId(order.getId());
        orderRefund.setUserId(order.getUserId());
        orderRefund.setRefundStatus(OrderRefundStatus.RETURNING);
        orderRefund.setApplyAt(Date.from(Instant.now()));
        refundMapper.insert(orderRefund);
    }

    private void updateOrderProduct(Long orderId, RefundType refundType, String orderProductIds) {
        List<String> orderProductIdList = null;
        if (Objects.equals(RefundType.PART, refundType)) {
            //写退款订单商品标识
            orderProductIdList = Arrays.asList(StringUtils.tokenizeToStringArray(orderProductIds, ","));
        }
        productMapper.updateOrderProductByIds(Maps.of("orderId", orderId,
                "refundStatus", RefundStatus.REFUND,
                "orderProductIds", orderProductIdList));
    }

    /**
     * 第三方退款
     *
     * @param payId 支付记录Id
     * @param param 退款入参
     */
    public void refund(String payId, ReturnOrderParam param) {
        RefundParam refundParam = new RefundParam();
        refundParam.setFee(param.getFee());
        refundParam.setReason(param.getReason());
        refundParam.setNotifyUrl(param.getNotifyUrl());
        ResponseEntity response = paymentService.refund(payId, refundParam);
        if (response.getStatusCode().isError()) {
            throw new ServiceException("退款失败");
        }
    }

    /**
     * 未发送海鼎，订单退款
     *
     * @param orderCode 订单编号
     * @param param     退款入参
     */
    public void notSendHdRefund(String orderCode, ReturnOrderParam param) {
        //更新订单状态
        int count = baseOrderMapper.updateStatusByCode(Maps.of("nowStatus", OrderStatus.WAIT_SEND_OUT
                , "modifyStatus", OrderStatus.RETURNING, "orderCode", orderCode));
        if (count == 1) {
            OrderDetailResult order = baseOrderMapper.selectByCode(orderCode);
            //更新订单商品状态
            this.updateOrderProduct(order.getId(), param.getRefundType(), param.getOrderProductIds());
            //记录退款日志
            this.insertRefundLog(order, param);
            this.refund(order.getPayId(), param);
            return;
        }
        throw new ServiceException("订单退款，更新状态失败");
    }

    /**
     * 发送海鼎未备货退款
     *
     * @param orderCode 订单编号
     * @param param     退款入参
     */
    public void sendHdRefund(String orderCode, ReturnOrderParam param) {
        //更新订单状态
        int count = baseOrderMapper.updateStatusByCode(Maps.of("nowStatus", OrderStatus.SEND_OUTING
                , "modifyStatus", OrderStatus.RETURNING, "orderCode", orderCode));
        if (count == 1) {
            OrderDetailResult order = baseOrderMapper.selectByCode(orderCode);
            //更新订单商品状态
            this.updateOrderProduct(order.getId(), param.getRefundType(), param.getOrderProductIds());
            //记录退款日志
            this.insertRefundLog(order, param);

            ResponseEntity hdResponse = haiDingService.hdCancel(orderCode, param.getReason());
            if (hdResponse.getStatusCode().isError()) {
                throw new ServiceException("海鼎取消失败，订单退款失败");
            }
            this.refund(order.getPayId(), param);
            return;
        }
        throw new ServiceException("更新订单退款状态失败");
    }

    /**
     * 退货退款，提交海鼎退货申请
     *
     * @param orderCode 订单编号
     * @param param     退货入参
     */
    public void applyHdReturns(String orderCode, ReturnOrderParam param) {
        //更新订单状态
        int count = baseOrderMapper.updateStatusToReturning(orderCode);
        if (count == 1) {
            OrderDetailResult order = baseOrderMapper.selectByCode(orderCode);
            //更新订单商品状态
            this.updateOrderProduct(order.getId(), param.getRefundType(), param.getOrderProductIds());
            //添加退款日志
            this.insertRefundLog(order, param);
            //提交海鼎退货申请
            this.hdReturns(order, param);
            return;
        }
        throw new ServiceException("退货失败");
    }

    /**
     * 退款回调确认
     *
     * @param orderCode    订单编号
     * @param refundStatus 退款回调状态
     * @return Tips
     */
    public Tips confirmRefund(String orderCode, OrderRefundStatus refundStatus) {
        OrderStatus modifyStatus = OrderStatus.ALREADY_RETURN;
        if (Objects.equals(OrderRefundStatus.RETURN_FAILURE, refundStatus)) {
            modifyStatus = OrderStatus.RETURN_FAILURE;
        }
        int count = baseOrderMapper.updateStatusByCode(Maps.of("nowStatus", OrderStatus.RETURNING, "modifyStatus", modifyStatus, "orderCode", orderCode));
        if (count == 1) {
            OrderDetailResult order = baseOrderMapper.selectByCode(orderCode);
            count = refundMapper.updateByOrderId(Maps.of("orderId", order.getId(), "refundStatus", refundStatus));
            if (count == 1) {
                return Tips.empty();
            }
            throw new ServiceException("修改订单退款记录失败");
        }
        return Tips.warn("确认退款失败");
    }

    /**
     * 海鼎退货退款
     *
     * @param order       订单信息
     * @param refundParam 退货信息
     * @return Pair
     */
    private void hdReturns(OrderDetailResult order, ReturnOrderParam refundParam) {
        HaiDingOrderParam haiDingOrderParam = new HaiDingOrderParam();
        BeanUtils.of(haiDingOrderParam).populate(order);
        List<OrderProduct> refundProducts = productMapper.selectOrderProductsByIds(Arrays.asList(StringUtils.tokenizeToStringArray(refundParam.getOrderProductIds(), ",")));
        OrderStore store = order.getOrderStore();
        haiDingOrderParam.setStoreName(store.getStoreName());
        haiDingOrderParam.setStoreCode(store.getStoreCode());
        haiDingOrderParam.setStoreId(store.getStoreId());
        haiDingOrderParam.setReturnReason(refundParam.getReason());
        haiDingOrderParam.setOrderProducts(refundProducts);
        ResponseEntity refundResponse = haiDingService.hdRefund(haiDingOrderParam);
        if (Objects.isNull(refundResponse) || refundResponse.getStatusCode().isError()) {
            throw new CustomFeignException(refundResponse);
        }
    }

    /**
     * 计算应退金额
     *
     * @param order      订单详情
     * @param productIds 应退商品Id集合
     * @return Integer
     */
    public Integer fee(OrderDetailResult order, String productIds) {
        final int[] fee = {0};
        List<OrderProduct> list = order.getOrderProductList();
        List<String> idList = Arrays.asList(StringUtils.tokenizeToStringArray(productIds, ","));
        idList.forEach(id ->
                list.stream().filter(product -> Objects.equals(id, product.getId().toString()))
                        .forEach(product -> fee[0] = fee[0] + product.getDiscountPrice()));
        if (Objects.equals(ReceivingWay.TO_THE_HOME, order.getReceivingWay()) && !Objects.equals(OrderStatus.RECEIVED, order.getStatus())) {
            fee[0] += order.getDeliveryAmount();
        }
        return fee[0];
    }

}
