package com.lhiot.oc.basic.feign;

import com.leon.microx.common.wrapper.Multiple;
import com.lhiot.oc.basic.feign.domain.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Component
@FeignClient("basic-data-center-v1-0-0")
public interface BaseServiceFeign {

    //TODO

    /**
     * @param key   Ids表示用standardIds查询，barcode表示用code查询
     * @param value 表示查询集合 以“,”隔开
     *              * @return
     */
    @RequestMapping(value = "/wxsmall/products/standards/find/{key}/{value}", method = RequestMethod.GET)
    ResponseEntity<Multiple<ProductsStandard>> productByStandardIds(@PathVariable("key") String key, @PathVariable("value") String value);

    @RequestMapping(value = "/stores/{id}", method = RequestMethod.GET)
    ResponseEntity<StoreInfo> storeById(@PathVariable("id") Long id);

    @RequestMapping(value = "/stores/by-code/{code}", method = RequestMethod.GET)
    ResponseEntity<StoreInfo> storeByCode(@PathVariable("code") String code);

    //批量查询门店信息
    @RequestMapping(value = "/stores/names", method = RequestMethod.GET)
    ResponseEntity<Multiple<StoreInfo>> findStoreByIds(@RequestParam("storeIds") String storeIds);

    //根据套餐id(逗号分割，如：12,23)查询商品信息
    @RequestMapping(value = "/assortment/list/{ids}", method = RequestMethod.GET)
    ResponseEntity<Multiple<Assortment>> findAssortments(@PathVariable("ids") String ids, @RequestParam("flag") String flag);

    //查询系统参数
    @RequestMapping(value = "/systemSetting/findByName/{name}", method = RequestMethod.GET)
    ResponseEntity<SystemSetting> findByName(@PathVariable("name") String name);
}
