package com.lhiot.oc.payment.api;

import com.leon.microx.web.swagger.ApiParamType;
import com.lhiot.oc.payment.entity.Record;
import com.lhiot.oc.payment.model.PayedModel;
import com.lhiot.oc.payment.service.PaymentService;
import com.lhiot.oc.payment.type.PayStep;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;

@Slf4j
@Validated
@RestController
@Api("支付记录接口")
public class RecordsApi {
    private PaymentService service;

    public RecordsApi(PaymentService service) {
        this.service = service;
    }

    @GetMapping("/users/{userId}/records")
    @ApiOperation(value = "分页查询用户支付记录集合", response = Record.class, responseReference = "Pages")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = ApiParamType.PATH, name = "userId", value = "用户ID", dataType = "Long", required = true),
            @ApiImplicitParam(paramType = ApiParamType.QUERY, name = "page", value = "分页 - 下标", dataType = "Integer"),
            @ApiImplicitParam(paramType = ApiParamType.QUERY, name = "rows", value = "分页 - 行数", dataType = "Integer"),
            @ApiImplicitParam(paramType = ApiParamType.QUERY, name = "step", value = "支付步骤", dataTypeClass = PayStep.class)
    })
    public ResponseEntity myRecords(@Valid @DecimalMin("1") @PathVariable("userId") Long userId,
                                    @RequestParam(value = "page", required = false) Integer page,
                                    @RequestParam(value = "rows", required = false) Integer rows,
                                    @RequestParam(value = "step", required = false) PayStep step) {
        return ResponseEntity.ok().body(service.myRecords(userId, page, rows, step));
    }

    @PutMapping("/records/{outTradeNo}/completed")
    @ApiOperation("修改支付单为完成状态")
    public ResponseEntity completed(@PathVariable("outTradeNo") Long outTradeNo, @Valid @RequestBody PayedModel payed) {
        boolean updated = service.completed(outTradeNo, payed);
        return updated ? ResponseEntity.ok().build() : ResponseEntity.badRequest().body("修改失败");
    }
}
