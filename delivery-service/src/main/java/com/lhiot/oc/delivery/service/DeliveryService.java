package com.lhiot.oc.delivery.service;

import com.leon.microx.util.Jackson;
import com.lhiot.oc.delivery.client.AdaptableClient;
import com.lhiot.oc.delivery.entity.DeliverFlow;
import com.lhiot.oc.delivery.entity.DeliverNote;
import com.lhiot.oc.delivery.feign.BasicDataService;
import com.lhiot.oc.delivery.feign.Store;
import com.lhiot.oc.delivery.model.DeliverOrder;
import com.lhiot.oc.delivery.model.DeliverStatus;
import com.lhiot.oc.delivery.model.DeliverType;
import com.lhiot.oc.delivery.model.DeliverUpdate;
import com.lhiot.oc.delivery.repository.DeliverBaseOrderMapper;
import com.lhiot.oc.delivery.repository.DeliverFlowMapper;
import com.lhiot.oc.delivery.repository.DeliverNoteMapper;
import com.lhiot.oc.delivery.repository.DeliverOrderProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Leon (234239150@qq.com) created in 16:20 18.11.10
 */
@Service
public class DeliveryService implements ApplicationContextAware {

    private ApplicationContext context;

    private final BasicDataService basicDataService;
    private final DeliverNoteMapper deliverNoteMapper;
    private final DeliverFlowMapper deliverFlowMapper;
    private final DeliverBaseOrderMapper deliverBaseOrderMapper;
    private final DeliverOrderProductMapper deliverOrderProductMapper;

    @Autowired
    public DeliveryService(BasicDataService basicDataService, DeliverNoteMapper deliverNoteMapper, DeliverFlowMapper deliverFlowMapper, DeliverBaseOrderMapper deliverBaseOrderMapper, DeliverOrderProductMapper deliverOrderProductMapper) {
        this.basicDataService = basicDataService;
        this.deliverNoteMapper = deliverNoteMapper;
        this.deliverFlowMapper = deliverFlowMapper;
        this.deliverBaseOrderMapper = deliverBaseOrderMapper;
        this.deliverOrderProductMapper = deliverOrderProductMapper;
    }

    public Optional<Store> store(String storeCode, String applicationType) {
        ResponseEntity response = basicDataService.findStoreByCode(storeCode, applicationType);
        if (response.getStatusCode().isError() || Objects.isNull(response.getBody())) {
            return Optional.empty();
        }
        return Optional.of((Store) response.getBody());
    }

    @Nullable
    public AdaptableClient adapt(DeliverType deliverType) {
        return (AdaptableClient) context.getBean(deliverType.getClientClass());
    }

    public DeliverNote deliverNote(String deliverCode) {
        return this.deliverNoteMapper.selectByDeliverCode(deliverCode);
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
        this.context = applicationContext;
    }

    public void saveDeliverFlow(DeliverNote deliverNote, DeliverStatus preStatus) {
        DeliverFlow deliverFlow = new DeliverFlow();
        deliverFlow.setCreateAt(new Date());
        deliverFlow.setDeliverNoteId(deliverNote.getId());
        deliverFlow.setPreStatus(preStatus);
        deliverFlow.setStatus(deliverNote.getDeliverStatus());
        deliverFlowMapper.create(deliverFlow);
        if (Objects.nonNull(preStatus)) {
            deliverNoteMapper.updateById(deliverNote);
        }
    }

    public void saveDeliverNote(DeliverOrder deliverOrder, DeliverNote deliverNote) {
        //创建配送单
        deliverNote.setDeliverStatus(DeliverStatus.CREATE);
        deliverNote.setCreateTime(new Date());
        deliverNoteMapper.create(deliverNote);

        //创建第一条（上一步状态为null）记录配送状态流水
        this.saveDeliverFlow(deliverNote, null);

        //写入配送订单流程表 如果查询到，就不新增
        if (Objects.isNull(this.deliverBaseOrderMapper.selectByHdOrderCode(deliverOrder.getHdOrderCode()))) {
            Jackson.json(deliverOrder.getDeliverTime());
            deliverBaseOrderMapper.create(deliverOrder);
            //给配送订单商品设置配送订单id
            deliverOrder.getDeliverOrderProductList().forEach(item -> item.setDeliverBaseOrderId(deliverOrder.getId()));
            deliverOrderProductMapper.createInBatch(deliverOrder.getDeliverOrderProductList());
        }
    }

    public void updateDeliverNote(DeliverNote deliverNote, DeliverUpdate deliverUpdate) {
        switch (deliverNote.getDeliverStatus()) {
            // 待取货
            case WAIT_GET:
                deliverNote.setReceiveTime(new Date());
                break;
            // 配送失败
            case FAILURE:
                deliverNote.setCancelTime(new Date());
                break;
            // 待接单 配送中 配送完成 直接修改。
            default:
                break;
        }
        DeliverStatus status = deliverNote.getDeliverStatus();
        deliverNote.setDeliverName(deliverUpdate.getCarrierDriverName());
        deliverNote.setDeliverPhone(deliverUpdate.getCarrierDriverPhone());
        deliverNote.setDeliverStatus(deliverUpdate.getDeliverStatus());
        deliverNote.setFailureCause(deliverUpdate.getCancelReason());
        this.saveDeliverFlow(deliverNote, status);

    }
}
