package com.lhiot.oc.basic.api;

import com.leon.microx.support.result.Tips;
import com.leon.microx.util.Jackson;
import com.leon.microx.util.StringUtils;
import com.lhiot.oc.basic.domain.enums.ApplicationTypeEnum;
import com.lhiot.oc.basic.domain.enums.NormalExchange;
import com.lhiot.oc.basic.domain.enums.OrderType;
import com.lhiot.oc.basic.feign.BaseUserServerFeign;
import com.lhiot.oc.basic.feign.domain.BaseUser;
import com.lhiot.order.domain.BaseOrderInfo;
import com.lhiot.order.domain.enums.ApplicationTypeEnum;
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
    private final RabbitTemplate rabbit;
    private final BaseUserServerFeign baseUserServerFeign;
    @Autowired
    public CurrencyPaymentApi(RabbitTemplate rabbit, BaseUserServerFeign baseUserServerFeign) {
        this.rabbit = rabbit;
        this.baseUserServerFeign = baseUserServerFeign;
    }

    @ApiOperation(value = "鲜果币支付接口")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "baseUserId", value = "基础用户ID", dataType = "Long", required = true),
            @ApiImplicitParam(paramType = "query", name = "orderCode", value = "订单编码", dataType = "String", required = true),
            @ApiImplicitParam(paramType = "query", name = "memo", value = "鲜果币扣款备注", dataType = "String", required = true),
            @ApiImplicitParam(paramType = "query", name = "applicationType", value = "应用类型", dataType = "ApplicationTypeEnum", required = true),
    })
    @GetMapping("/currency/pay")
    public ResponseEntity<?> currencyPay(@RequestParam("id") Long baseUserId,
                                         @RequestParam("orderCode") String orderCode,
                                         @RequestParam("memo") String memo,
                                         @RequestParam("applicationType") ApplicationTypeEnum applicationType){
        if (baseUserId == null) {
            return ResponseEntity.badRequest().body(Tips.of("-1", "基础用户ID为空"));
        }
        if (StringUtils.isBlank(memo)) {
            return ResponseEntity.badRequest().body(Tips.of("-1", "鲜果币扣款备注为空"));
        }
        if (StringUtils.isBlank(orderCode)) {
            return ResponseEntity.badRequest().body(Tips.of("-1", "订单编码为空"));
        }

        BaseUser baseUser=new BaseUser();
        baseUser.setApplicationType(applicationType);
        baseUser.setId(baseUserId);
        return baseUserServerFeign.updateCurrencyById(baseUserId,memo,baseUser);

    }
}
