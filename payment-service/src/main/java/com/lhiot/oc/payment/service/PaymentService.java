package com.lhiot.oc.payment.service;

import com.leon.microx.exception.ServiceException;
import com.leon.microx.id.Generator;
import com.leon.microx.pay.model.SignAttrs;
import com.leon.microx.pay.type.TradeType;
import com.leon.microx.util.Maps;
import com.leon.microx.web.result.Pages;
import com.lhiot.oc.payment.entity.Record;
import com.lhiot.oc.payment.feign.*;
import com.lhiot.oc.payment.mapper.RecordMapper;
import com.lhiot.oc.payment.model.AliPayModel;
import com.lhiot.oc.payment.model.BalancePayModel;
import com.lhiot.oc.payment.model.PaidModel;
import com.lhiot.oc.payment.model.WxPayModel;
import com.lhiot.oc.payment.type.PayStep;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Leon (234239150@qq.com) created in 9:20 18.12.3
 */
@Service
@Transactional
public class PaymentService {

    private static final int DEFAULT_SIGN_TTL = 6;

    private BaseDataService dataService;

    private RecordMapper recordMapper;

    private Generator<Long> idGenerator;

    private BaseUserService userService;

    public PaymentService(BaseDataService dataService, RecordMapper recordMapper, Generator<Long> idGenerator, BaseUserService userService) {
        this.dataService = dataService;
        this.recordMapper = recordMapper;
        this.idGenerator = idGenerator;
        this.userService = userService;
    }

    public PaymentConfig findPaymentConfig(String configName) {
        ResponseEntity response = dataService.findPaymentConfig(configName);
        if (response.getStatusCode().isError() || Objects.isNull(response.getBody())) {
            return null;
        }
        return (PaymentConfig) response.getBody();
    }

    public PaymentConfig findPaymentConfig(Long outTradeNo) {
        Record record = recordMapper.one(outTradeNo);
        if (Objects.isNull(record)) {
            return null;
        }
        return findPaymentConfig(record.getConfigName());
    }

    private User findUser(Long userId) {
        ResponseEntity findUserResponse = userService.findUser(userId);
        if (findUserResponse.getStatusCode().isError() || Objects.isNull(findUserResponse.getBody())) {
            throw new ServiceException("获取用户信息失败");
        }
        return (User) findUserResponse.getBody();
    }

    public long balancePay(BalancePayModel balancePay) {
        User payUser = this.findUser(balancePay.getUserId());
        Record record = Record.from(idGenerator.get(), payUser, balancePay);
        record.setOrderCode(balancePay.getOrderCode());
        record.setSignAt(null);             // 余额支付时，签名时间为空
        record.setPayStep(PayStep.PAID);   // 余额支付直接为支付完成状态
        record.setTradeType(TradeType.OTHER_PAY);
        record.setPayAt(Date.from(Instant.now()));
        if (recordMapper.insert(record) == 1) {
            Balance balance = new Balance();
            balance.setMoney(balancePay.getFee());
            balance.setApplicationType(balancePay.getApplicationType());
            balance.setOperation(Balance.Operation.SUBTRACT);
            balance.setPassword(balancePay.getPassword());
            balance.setSourceId(String.valueOf(record.getId()));
            balance.setMemo("订单余额支付");
            ResponseEntity response = userService.updateBalance(balancePay.getUserId(), balance);
            if (response.getStatusCode().isError()) {
                throw new ServiceException("扣除用户鲜果币失败");
            }
            return record.getId();
        }
        return 0;
    }

    /**
     * 准备微信支付。初始化支付记录、生成签名参数对象
     *
     * @param wxPay     微信支付对象
     * @param tradeType 支付签名类型
     * @return 签名参数对象
     */
    public SignAttrs ready(WxPayModel wxPay, TradeType tradeType) {
        User payUser = this.findUser(wxPay.getUserId());
        Record record = Record.from(idGenerator.get(), payUser, wxPay);
        record.setOrderCode(wxPay.getOrderCode());
        record.setTradeType(tradeType);
        record.setConfigName(wxPay.getConfigName());
        record.setOpenId(wxPay.getOpenid());
        record.setClientIp(wxPay.getClientIp());
        return this.ready(record);
    }

    /**
     * 准备支付宝支付。初始化支付记录、生成签名参数对象
     *
     * @param aliPay 支付宝支付对象
     * @return 签名参数对象
     */
    public SignAttrs ready(AliPayModel aliPay) {
        User payUser = this.findUser(aliPay.getUserId());
        Record record = Record.from(idGenerator.get(), payUser, aliPay);
        record.setOrderCode(aliPay.getOrderCode());
        record.setTradeType(TradeType.ALI_APP);
        record.setConfigName(aliPay.getConfigName());
        return this.ready(record);
    }

    private SignAttrs ready(Record record) {
        if (recordMapper.insert(record) < 1) {
            throw new ServiceException("创建支付记录失败");
        }
        return SignAttrs.builder()
                .outTradeNo(String.valueOf(record.getId()))
                .title(record.getMemo())
                .content(record.getMemo())
                .cnyCent(record.getFee())
                .attachJson(record.getAttach())
                .ttlMinutes(DEFAULT_SIGN_TTL)
                .build();
    }

    public Pages<Record> myRecords(Long userId, Integer page, Integer rows, PayStep step) {
        int total;
        List<Record> records;
        if (Objects.isNull(rows) || rows == -1) {
            records = recordMapper.selectList(Maps.of("userId", userId, "step", step));
            total = records.size();
        } else {
            Map<String, Object> param = Maps.of(
                    "userId", userId, "step", step, "start", ((Objects.isNull(page) || page == 0 ? 1 : page) - 1) * rows, "rows", rows
            );
            records = recordMapper.selectPages(param);
            total = recordMapper.count(param);
        }
        return Pages.of(total, records);
    }

    public Record record(Long id) {
        return recordMapper.one(id);
    }

    public boolean completed(Long outTradeNo, PaidModel paid) {
        return recordMapper.completed(
                Maps.of(
                        "id", outTradeNo,
                        "tradeId", paid.getTradeId(),
                        "bankType", paid.getBankType(),
                        "payAt", paid.getPayAt(),
                        "payStep", PayStep.PAID
                )
        ) == 1;
    }
}
