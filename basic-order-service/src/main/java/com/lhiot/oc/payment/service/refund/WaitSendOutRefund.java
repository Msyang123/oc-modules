package com.lhiot.oc.delivery.service.refund;

import com.leon.microx.common.wrapper.Tips;
import com.leon.microx.util.Calculator;
import com.leon.microx.util.StringUtils;
import com.lhiot.order.domain.BaseOrderInfo;
import com.lhiot.order.domain.OrderProduct;
import com.lhiot.order.domain.enums.OrderStatus;
import com.lhiot.order.domain.inparam.ReturnOrderParam;
import com.lhiot.order.domain.payment.PaymentLog;
import com.lhiot.order.feign.ThirdPartyServiceFeign;
import com.lhiot.order.service.BaseOrderService;
import com.lhiot.order.service.payment.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 待发货订单退货处理
 *
 * @author liuyo on 17.8.25.
 */
@Slf4j
@Component("waitSendOutRefund")
public class WaitSendOutRefund implements IOrderRefund {

    private static final String HD_CANCEL_ORDER_SUCCESS_RESULT_STRING = "{\"success\":true}";

    private final PaymentService paymentService;

    private final BaseOrderService baseOrderService;

    private final ThirdPartyServiceFeign thirdPartyServiceFeign;

    @Autowired
    public WaitSendOutRefund(PaymentService paymentService,
                             BaseOrderService baseOrderService, ThirdPartyServiceFeign thirdPartyServiceFeign) {
        this.paymentService = paymentService;
        this.baseOrderService = baseOrderService;
        this.thirdPartyServiceFeign = thirdPartyServiceFeign;
    }
    public Tips doRefund(BaseOrderInfo orderInfo, ReturnOrderParam data) throws Exception {
        if (Objects.isNull(orderInfo)) {
            return Tips.of(-1, "未找到订单信息");
        }

        //查询支付中心此订单的支付记录情况
        PaymentLog paymentLog = orderInfo.getPaymentLog();
        if (Objects.isNull(paymentLog)) {
            return Tips.of(-1, "订单支付信息为空");
        }
        //由于存在调货场景，此处使用海鼎订单编号进行海鼎操作
        ResponseEntity<String> responseEntity = thirdPartyServiceFeign.hdOrderCancel(orderInfo.getHdOrderCode(), StringUtils.replaceEmoji(data.getReturnReason(), ""));
        String result = responseEntity.getBody();
        if (!Objects.equals(HD_CANCEL_ORDER_SUCCESS_RESULT_STRING, result)) {
            return Tips.of(-1, "退货失败");
        }

        String[] standardIds = data.getOrderBarcodeIds().split(",");
        //计算部分退货商品总金额
        int partGoodFee = 0;
        for (OrderProduct item : orderInfo.getOrderProducts()) {
            for (String standardId : standardIds) {
                if (item.getStandardId().equals(Long.valueOf(standardId))) {
                    partGoodFee += Calculator.mul(item.getDiscountPrice(), item.getProductQty());//去除优惠后的金额累加
                    break;
                }
            }
        }
        //TODO 此处传的订单Id 与签名保持一致  待讨论
        Tips refundResult = paymentService.refundOrder(data);
        if (null == refundResult || Objects.equals(refundResult.getCode(), "-1")) {
            log.error("订单退款调用远端支付中心退款失败" + orderInfo.getHdOrderCode());
            return Tips.of(-1, "退货失败");
        }
        paymentService.recordOrderRefund(data.getUserId(), orderInfo.getId(), orderInfo.getCode(), data.getReturnReason(), partGoodFee, paymentLog.getTradeId());
        //修改订单和订单商品状态
        boolean updateFlag = baseOrderService.refundUpdateOrderAndGoods(data, OrderStatus.ALREADY_RETURN, orderInfo.getStatus(), null);
        //只有订单状态修改成功才返还用户鲜果币
        if (updateFlag) {
            return Tips.of(1, "success");
        } else {
            return Tips.of(1, "退货失败");
        }
    }
}
