package com.lhiot.oc.basic.feign;

import com.lhiot.oc.basic.feign.domain.BaseUser;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;


/**
 * 用户中心基础服务
 * Created by yj
 */
@FeignClient("base-user-service-v1-0-0")
@Component
public interface BaseUserServerFeign {


	/**
	 * 根据id查询公共用户
	 */
    @RequestMapping(value="/baseUser/{id}",method = RequestMethod.GET)
    ResponseEntity<BaseUser> findBaseUserById(@PathVariable("id") Long id);

	/**
	 * 更新公共用户鲜果币
	 */
	@RequestMapping(value="/baseUser/updateCurrencyById/{id}",method = RequestMethod.PUT)
    ResponseEntity<Object> updateCurrencyById(@PathVariable("id") Long id, @RequestParam("memo") String memo, @RequestBody BaseUser baseUser);

}
