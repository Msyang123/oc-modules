package com.lhiot.oc.basic.api;

import com.leon.microx.support.result.Tips;
import com.leon.microx.util.StringUtils;
import com.lhiot.oc.basic.domain.SignParam;
import com.lhiot.oc.basic.feign.BaseUserServerFeign;
import com.lhiot.oc.basic.feign.domain.BalanceOperationParam;
import com.lhiot.oc.basic.feign.domain.UserDetailResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * @author
 */
@Slf4j
@RestController
@Api("鲜果币公共支付api")
@RequestMapping("/currency")
public class CurrencyPaymentApi {
    private final BaseUserServerFeign baseUserServerFeign;
    @Autowired
    public CurrencyPaymentApi(BaseUserServerFeign baseUserServerFeign) {
        this.baseUserServerFeign = baseUserServerFeign;
    }

    @ApiOperation(value = "鲜果币支付-充值接口")
    @ApiImplicitParam(paramType = "body", name = "signParam", dataType = "SignParam", required = true, value = "鲜果币支付传入参数")
    @PostMapping("/pay")
    public ResponseEntity<?> currencyPay(@RequestBody SignParam signParam){
        if(Objects.isNull(signParam)){
            return ResponseEntity.badRequest().body(Tips.of("-1", "支付传递参数为空"));
        }
        if (signParam.getBaseuserId() == null) {
            return ResponseEntity.badRequest().body(Tips.of("-1", "基础用户ID为空"));
        }
        ResponseEntity<UserDetailResult> baseUser = baseUserServerFeign.findBaseUserById(signParam.getBaseuserId());
        if(Objects.isNull(baseUser)){
            return ResponseEntity.badRequest().body(Tips.of("-1", "基础用户不存在"));
        }
        if (StringUtils.isBlank(signParam.getMemo())) {
            return ResponseEntity.badRequest().body(Tips.of("-1", "鲜果币扣款备注为空"));
        }
        if (Objects.isNull(signParam.getFee())) {
            return ResponseEntity.badRequest().body(Tips.of("-1", "支付金额为空"));
        }
        if (signParam.getFee()<=0) {
            return ResponseEntity.badRequest().body(Tips.of("-1", "支付金额不能小于0"));
        }
        if(Objects.isNull(signParam.getOperation())){
            return ResponseEntity.badRequest().body(Tips.of("-1", "鲜果币支付必须传递操作类型"));
        }

        BalanceOperationParam balanceOperationParam=new BalanceOperationParam();
        balanceOperationParam.setBaseUserId(signParam.getBaseuserId());
        balanceOperationParam.setApplicationType(signParam.getApplicationType());
        balanceOperationParam.setMoney(signParam.getFee());
        balanceOperationParam.setOperation(signParam.getOperation());
        balanceOperationParam.setSourceId(signParam.getPayCode());
        balanceOperationParam.setSourceType(signParam.getMemo());
        //扣减或者充值鲜果币
        return baseUserServerFeign.updateCurrencyById(balanceOperationParam);
    }
}
