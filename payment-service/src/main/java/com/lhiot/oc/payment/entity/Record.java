package com.lhiot.oc.payment.entity;

import com.leon.microx.pay.type.TradeType;
import com.lhiot.oc.payment.feign.User;
import com.lhiot.oc.payment.model.PayModel;
import com.lhiot.oc.payment.type.PayStep;
import com.lhiot.oc.payment.type.SourceType;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.time.Instant;
import java.util.Date;

@Data
@ToString
public class Record implements Serializable {

    private Long id;

    private Long userId;

    private String userPhone;

    private String userRealName;

    private String userIdCard;

    private String orderCode;

    private String applicationType;

    private TradeType tradeType;

    private SourceType sourceType;

    private String configName;

    private Long fee;

    private PayStep payStep;

    private String tradeId;

    private Date signedAt;

    private Date paidAt;

    private String memo;

    private String bankType;

    private String openId;

    private String clientIp;

    private String attach;

    public static Record from(Long outTradeNo, User user, PayModel payModel){
        Record record = new Record();
        record.setId(outTradeNo);
        record.setUserId(user.getId());
        record.setUserPhone(user.getPhone());
        record.setUserRealName(user.getRealName());
        record.setUserIdCard(user.getIdCard());
        record.setApplicationType(payModel.getApplicationType());
        record.setSourceType(payModel.getSourceType());
        record.setFee(payModel.getFee());
        record.setMemo(payModel.getMemo());
        record.setPayStep(PayStep.SIGN);
        record.setSignedAt(Date.from(Instant.now()));
        record.setAttach(payModel.getAttach());
        return record;
    }
}
