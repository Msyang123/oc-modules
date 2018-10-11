package com.lhiot.oc.payment.feign;

import com.lhiot.oc.payment.feign.domain.BalanceOperationParam;
import com.lhiot.oc.payment.feign.domain.BaseUserResult;
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
	 * 根据业务用户id查询用户
	 */
    @RequestMapping(value="/users/{userId}",method = RequestMethod.GET)
    ResponseEntity<UserDetailResult> findUserById(@PathVariable("userId") Long userId);

	/**
	 * 依据基础用户id查询基础用户
	 * @param baseUserId
	 * @return
	 */
/*	@RequestMapping(value="/users/base-user/{baseUserId}",method = RequestMethod.GET)
	ResponseEntity<BaseUserResult> findBaseUserById(@PathVariable("baseUserId") Long baseUserId);*/


	/**
	 * 更新公共用户鲜果币
	 */
	@RequestMapping(value="/users/{id}/balance",method = RequestMethod.PUT)
    ResponseEntity updateCurrencyById(@PathVariable("id") long id,@RequestBody BalanceOperationParam param);


	/**
	 * 查询用户余额
	 */
	@RequestMapping(value="/users/{id}/balance",method = RequestMethod.GET)
	ResponseEntity<Long> findFruitCurrency(@PathVariable("id") Long id);

	/**
	 * 判断是否可使用余额支付
	 */
/*	@RequestMapping(value="/users/payment-password",method = RequestMethod.POST)
	ResponseEntity determinePaymentPassword(@RequestBody PaymentPasswordParam param);*/

}
