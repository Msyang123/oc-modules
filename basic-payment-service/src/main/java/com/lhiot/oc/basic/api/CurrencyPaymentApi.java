package com.lhiot.oc.basic.api;

import com.leon.microx.common.wrapper.Tips;
import com.leon.microx.util.Jackson;
import com.leon.microx.util.StringUtils;
import com.lhiot.oc.basic.domain.enums.NormalExchange;
import com.lhiot.order.domain.BaseOrderInfo;
import com.lhiot.order.domain.enums.Apply;
import com.lhiot.order.domain.enums.NormalExchange;
import com.lhiot.order.domain.enums.OrderStatus;
import com.lhiot.order.domain.enums.OrderType;
import com.lhiot.order.service.BaseOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * @author
 */
@Slf4j
@RestController
@Api("鲜果币公共支付api")
@RequestMapping("/currencypayment")
public class CurrencyPaymentApi {
    private final BaseOrderService baseOrderService;
    private final RabbitTemplate rabbit;
    @Autowired
    public CurrencyPaymentApi(BaseOrderService baseOrderService, RabbitTemplate rabbit) {
        this.baseOrderService = baseOrderService;
        this.rabbit = rabbit;
    }

    @ApiOperation(value = "鲜果币支付订单接口")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "baseUserId", value = "基础用户ID", dataType = "Long", required = true),
            @ApiImplicitParam(paramType = "query", name = "orderCode", value = "订单编码", dataType = "String", required = true),
            @ApiImplicitParam(paramType = "query", name = "memo", value = "鲜果币扣款备注", dataType = "String", required = true),
            @ApiImplicitParam(paramType = "query", name = "apply", value = "应用类型", dataType = "Apply", required = true),
    })
    @GetMapping("/currency/pay")
    public ResponseEntity<?> currencyPay(@RequestParam("id") Long baseUserId,
                                         @RequestParam("orderCode") String orderCode,
                                         @RequestParam("memo") String memo,
                                         @RequestParam("apply") Apply apply) throws Exception {
        if (baseUserId == null) {
            return ResponseEntity.badRequest().body(Tips.of("-1", "基础用户ID为空"));
        }
        if (StringUtils.isBlank(memo)) {
            return ResponseEntity.badRequest().body(Tips.of("-1", "鲜果币扣款备注为空"));
        }
        if (StringUtils.isBlank(orderCode)) {
            return ResponseEntity.badRequest().body(Tips.of("-1", "订单编码为空"));
        }
        BaseOrderInfo baseOrderInfo = baseOrderService.findOrderByCode(orderCode);
        if (Objects.isNull(baseOrderInfo)) {
            return ResponseEntity.badRequest().body(Tips.of("-1", "未找到订单"));
        }
        //非待支付状态订单
        if (!Objects.equals(baseOrderInfo.getStatus(), OrderStatus.WAIT_PAYMENT)) {
            return ResponseEntity.badRequest().body(Tips.of("-1", "订单非待支付状态"));
        }
        //鲜果币支付订单
        Tips backMsg = baseOrderService.currencyPayOrder(baseOrderInfo, baseUserId, memo,apply);
        if (backMsg.getCode().equals("-1")) {
            return ResponseEntity.badRequest().body(backMsg.getMessage());
        } else {
            //发送到队列处理
            //如果发送海鼎减库存失败，则发送到队列中进行重试
            //团购订单不在此处发送海鼎 单独掉接口发送
            if(Objects.equals(baseOrderInfo.getOrderType(),OrderType.TEAM_BUYING)){
                return ResponseEntity.ok(backMsg);
            }else if(Objects.equals(baseOrderInfo.getOrderType(), OrderType.TO_STORE)){
                //购买到仓库
                //TODO 发送到个人仓库 不需要发送海鼎
                //baseUserServerFeign.xxxxx;
                return ResponseEntity.ok(backMsg);
            }else{
                //通过主题队列异步处理海鼎订单发送与配送业务
                rabbit.convertAndSend(NormalExchange.SEND_TO_HD.getExchangeName(), NormalExchange.SEND_TO_HD.getQueueName(), Jackson.json(baseOrderInfo));
                return ResponseEntity.ok(backMsg);
            }
        }
    }
}
