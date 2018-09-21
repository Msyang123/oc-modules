package com.lhiot.oc.payment.feign;

import com.lhiot.oc.payment.feign.domain.BalanceOperationParam;
import com.lhiot.oc.payment.feign.domain.PaymentPasswordParam;
import com.lhiot.oc.payment.feign.domain.UserDetailResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


/**
 * 用户中心基础服务
 * Created by yj
 */
@FeignClient("basic-user-service-v1-0")
@Component
public interface BaseUserServerFeign {


	/**
	 * 根据id查询公共用户
	 */
    @RequestMapping(value="/users/user-id/{userId}",method = RequestMethod.GET)
    ResponseEntity<UserDetailResult> findBaseUserById(@PathVariable("userId") Long userId);


	/**
	 * 更新公共用户鲜果币
	 */
	@RequestMapping(value="/users/balance/operation",method = RequestMethod.PUT)
    ResponseEntity updateCurrencyById(@RequestBody BalanceOperationParam param);


	/**
	 * 查询用户余额
	 */
	@RequestMapping(value="/users/balance/{id}",method = RequestMethod.GET)
	ResponseEntity<Long> findFruitCurrency(@PathVariable("id") Long baseUserId);

	/**
	 * 判断是否可使用余额支付
	 */
	@RequestMapping(value="/users/payment-password",method = RequestMethod.PUT)
	ResponseEntity determinePaymentPassword(@RequestBody PaymentPasswordParam param);

}
