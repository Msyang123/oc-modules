package com.lhiot.oc.delivery.service;

import com.lhiot.oc.delivery.client.AdaptableClient;
import com.lhiot.oc.delivery.entity.DeliverFlow;
import com.lhiot.oc.delivery.entity.DeliverNote;
import com.lhiot.oc.delivery.feign.BasicDataService;
import com.lhiot.oc.delivery.feign.Store;
import com.lhiot.oc.delivery.model.*;
import com.lhiot.oc.delivery.repository.DeliverBaseOrderMapper;
import com.lhiot.oc.delivery.repository.DeliverFlowMapper;
import com.lhiot.oc.delivery.repository.DeliverNoteMapper;
import com.lhiot.oc.delivery.repository.DeliverOrderProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
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

    public Optional<Store> store(Long storeId, ApplicationType applicationType) {
        ResponseEntity response = basicDataService.findStoreById(storeId, applicationType);
        if (response.getStatusCode().isError() || Objects.isNull(response.getBody())) {
            return Optional.empty();
        }
        return Optional.of((Store) response.getBody());
    }

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

    public void saveDeliverFlow(DeliverNote deliverNote) {
        if (Objects.nonNull(deliverNote.getDeliverStatus())) {
            DeliverFlow deliverFlow = new DeliverFlow();
            deliverFlow.setCreateAt(new Date());
            deliverFlow.setDeliverNoteId(deliverNote.getId());
            deliverFlow.setPreStatus(null);
            deliverFlow.setStatus(deliverNote.getDeliverStatus());
            deliverFlowMapper.create(deliverFlow);
            return;
        }
        deliverNoteMapper.updateById(deliverNote);
    }

    public void saveDeliverNote(DeliverOrder deliverOrder, DeliverNote deliverNote) {
        //创建配送单
        deliverNote.setDeliverStatus(DeliverStatus.CREATE);
        deliverNote.setCreateTime(new Date());
        deliverNoteMapper.create(deliverNote);

        //记录配送状态流水
        this.saveDeliverFlow(deliverNote);

        //更新配送信息
        DeliverNote updateDeliverNote = new DeliverNote();
        updateDeliverNote.setDeliverStatus(DeliverStatus.UNRECEIVE);
        updateDeliverNote.setId(deliverNote.getId());
        deliverNoteMapper.updateById(deliverNote);

        //写入配送订单流程表 如果查询到，就不新增
        if (Objects.isNull(this.deliverBaseOrderMapper.selectByHdOrderCode(deliverOrder.getHdOrderCode()))) {
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
                deliverNote.setDeliverName(deliverUpdate.getCarrierDriverName());
                deliverNote.setDeliverPhone(deliverUpdate.getCarrierDriverPhone());
                deliverNote.setReceiveTime(new Date());
                this.saveDeliverFlow(deliverNote);
                break;
            // 配送失败
            case FAILURE:
                deliverNote.setFailureCause(deliverUpdate.getCancelReason());
                deliverNote.setCancelTime(new Date());
                this.saveDeliverFlow(deliverNote);
                break;
            // 待接单 配送中 配送完成 直接修改。
            default:
                this.saveDeliverFlow(deliverNote);
                break;
        }
    }
}
