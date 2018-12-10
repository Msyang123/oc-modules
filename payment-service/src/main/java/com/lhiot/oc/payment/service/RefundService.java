package com.lhiot.oc.payment.service;

import com.leon.microx.exception.ServiceException;
import com.leon.microx.id.Generator;
import com.leon.microx.util.Maps;
import com.lhiot.oc.payment.entity.Record;
import com.lhiot.oc.payment.entity.Refund;
import com.lhiot.oc.payment.feign.Balance;
import com.lhiot.oc.payment.feign.BaseUserService;
import com.lhiot.oc.payment.mapper.RefundMapper;
import com.lhiot.oc.payment.model.RefundModel;
import com.lhiot.oc.payment.type.RefundStep;
import com.lhiot.oc.payment.type.SourceType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;

@Service
@Transactional
public class RefundService {

    private RefundMapper refundMapper;

    private Generator<Long> idGenerator;

    private BaseUserService userService;

    public RefundService(RefundMapper refundMapper, Generator<Long> idGenerator, BaseUserService userService) {
        this.refundMapper = refundMapper;
        this.idGenerator = idGenerator;
        this.userService = userService;
    }

    public boolean canRefund(Record record, RefundModel refund) {
        long historical = refundMapper.historicalAmount(record.getId());
        return record.getFee() >= (historical + refund.getFee()); // 支付金额 >= 历史退款总额 + 本次退还金额
    }

    public boolean balanceRefund(Record record, RefundModel refund) {
        this.createRefund(record, refund);
        Balance balance = new Balance();
        balance.setMoney(refund.getFee());
        balance.setApplicationType(record.getApplicationType());
        balance.setOperation(Balance.Operation.ADD);
        balance.setSourceId(record.getOrderCode());
        balance.setSourceType(SourceType.ORDER.name()); // 充值不给退款！！
        ResponseEntity response = userService.updateBalance(record.getUserId(), balance);
        return response.getStatusCode().is2xxSuccessful();
    }

    public long createRefund(Record record, RefundModel model) {
        long id = idGenerator.get();
        Refund refund = new Refund();
        refund.setId(id);
        refund.setRecordId(record.getId());
        refund.setCreateAt(Date.from(Instant.now()));
        refund.setFee(model.getFee());
        refund.setReason(model.getReason());
        refund.setRefundStep(RefundStep.SENT);
        if (refundMapper.insert(refund) < 1) {
            throw new ServiceException("创建退款记录失败！");
        }
        return id;
    }

    public boolean refundCompleted(Long refundId) {
        return refundMapper.completed(Maps.of(
                "id", refundId,
                "step", RefundStep.COMPLETED,
                "completedAt", Date.from(Instant.now())
        )) == 1;
    }
}
