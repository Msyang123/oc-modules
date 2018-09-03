package com.lhiot.oc.basic.service.refund;

import com.leon.microx.common.wrapper.Tips;
import com.lhiot.oc.basic.domain.BaseOrderInfo;
import com.lhiot.oc.basic.domain.OrderProduct;
import com.lhiot.oc.basic.domain.enums.OrderStatus;
import com.lhiot.oc.basic.domain.inparam.ReturnOrderParam;
import com.lhiot.oc.basic.feign.BaseServiceFeign;
import com.lhiot.oc.basic.feign.ThirdPartyServiceFeign;
import com.lhiot.oc.basic.feign.domain.StoreInfo;
import com.lhiot.oc.basic.service.BaseOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 已收货订单退货处理
 *
 * @author liuyo on 17.8.25.
 */
@Component("receivedRefund")
@Transactional
public class ReceivedRefund implements IOrderRefund {

    private final BaseOrderService baseOrderService;
    private final BaseServiceFeign baseServiceFeign;
    private final ThirdPartyServiceFeign thirdPartyServiceFeign;

    @Autowired
    public ReceivedRefund(BaseOrderService baseOrderService, BaseServiceFeign baseServiceFeign, ThirdPartyServiceFeign thirdPartyServiceFeign) {
        this.baseOrderService = baseOrderService;
        this.baseServiceFeign = baseServiceFeign;
        this.thirdPartyServiceFeign = thirdPartyServiceFeign;
    }
    @Override
    public Tips doRefund(BaseOrderInfo orderInfo, ReturnOrderParam data) throws Exception{
        if (Objects.isNull(orderInfo)) {
            return Tips.of(-1,"未找到订单信息");
        }
        List<OrderProduct> orderProducts = orderInfo.getOrderProducts();
        if(Objects.isNull(orderProducts) || orderProducts.isEmpty()){
        	 return Tips.of(-1, "传递订单规格编号不能为空！");
        }
        ResponseEntity<StoreInfo> storeResult = baseServiceFeign.storeById(orderInfo.getStoreId());
        if(storeResult == null || storeResult.getStatusCodeValue()>=400){
            return Tips.of(-1, "未找到门店");
        }
        orderInfo.setStoreName(storeResult.getBody().getStoreName());
        ResponseEntity<String> responseEntity = thirdPartyServiceFeign.hdOrderRefund(orderInfo);
        if (responseEntity == null || responseEntity.getStatusCodeValue() >= 400) {
            return Tips.of(-1, "退货失败");
        }
        //修改订单状态
        boolean flag = baseOrderService.refundUpdateOrderAndGoods(data, OrderStatus.RETURNING,orderInfo.getStatus(),orderInfo);
        return flag ? Tips.of(0, "success") : Tips.of(-1, "退货失败");
    }
}
