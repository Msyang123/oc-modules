package com.lhiot.oc.order.api;

import com.leon.microx.swagger.ApiParamType;
import com.lhiot.oc.order.model.RefundParam;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Leon (234239150@qq.com) created in 9:34 18.9.1
 */
@Slf4j
@RestController
@Api("退货API")
public class ReturnApi {

    @PostMapping("/part/return")
    @ApiOperation("部分退货")
    @ApiImplicitParam(paramType = ApiParamType.BODY, name = "refundParam", value = "部分退货参数对象", required = true, dataType = "RefundParam")
    public ResponseEntity partReturn(@RequestBody RefundParam refundParam) {
        log.info(refundParam.toString());   // 这里只是举个栗子
        return ResponseEntity.noContent().build();
    }
}
