package com.lhiot.oc.delivery.service;

import com.lhiot.oc.delivery.domain.DeliverFlow;
import com.lhiot.oc.delivery.domain.DeliverNote;
import com.lhiot.oc.delivery.domain.common.PagerResultObject;
import com.lhiot.oc.delivery.domain.enums.DeliveryStatus;
import com.lhiot.oc.delivery.mapper.DeliverFlowMapper;
import com.lhiot.oc.delivery.mapper.DeliverNoteMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
* Description:配送单信息服务类
* @author zhangshu 2018/08/06 created
*/
@Service
@Transactional
public class DeliveryNoteService {

    private final DeliverNoteMapper deliverNoteMapper;
    private final DeliverFlowMapper deliverFlowMapper;

    @Autowired
    public DeliveryNoteService(DeliverNoteMapper deliverNoteMapper, DeliverFlowMapper deliverFlowMapper) {
        this.deliverNoteMapper = deliverNoteMapper;
        this.deliverFlowMapper = deliverFlowMapper;
    }

    /** 
    * Description:根据id修改配送单信息
    */ 
    public int updateById(DeliverNote deliverNote){
        //如果是更新状态，那么就记录流水
        if(Objects.nonNull(deliverNote.getDeliverStatus())){
            DeliverNote searchDeliverNote = this.selectById(deliverNote.getId());
            DeliverFlow deliverFlow = new DeliverFlow();
            deliverFlow.setCreateAt(new Date());
            deliverFlow.setDeliverNoteId(searchDeliverNote.getId());
            deliverFlow.setPreStatus(searchDeliverNote.getDeliverStatus());
            deliverFlow.setStatus(deliverNote.getDeliverStatus());
            deliverFlowMapper.create(deliverFlow);
        }
        return this.deliverNoteMapper.updateById(deliverNote);
    }

    /** 
    * Description:根据ids删除配送单信息
    */ 
    public int deleteByIds(String ids){
        return this.deliverNoteMapper.deleteByIds(Arrays.asList(ids.split(",")));
    }
    
    /** 
    * Description:根据id查找配送单信息
    */ 
    public DeliverNote selectById(Long id){
        return this.deliverNoteMapper.selectById(id);
    }

    /**
     * 依据配送单编码查询
     * @param deliverCode deliverCode
     * @return DeliverNote
     */
    public DeliverNote selectByDeliverCode(String deliverCode){
        return this.deliverNoteMapper.selectByDeliverCode(deliverCode);
    }
    /**
     * Description:根据id查找配送单信息
     */
    public List<DeliverNote> selectByOrderId(Long orderId){
        return this.deliverNoteMapper.selectByOrderId(orderId);
    }

    /**
     * Description:根据orderId查找最新配送单信息
     */
    public DeliverNote selectLastByOrderId(Long orderId){
        return this.deliverNoteMapper.selectLastByOrderId(orderId);
    }


    /** 
    * Description: 查询配送单信息总记录数
    */  
    public long count(DeliverNote deliverNote){
        return this.deliverNoteMapper.pageDeliverNoteCounts(deliverNote);
    }
    
    /** 
    * Description: 查询配送单信息分页列表
    */  
    public PagerResultObject<DeliverNote> pageList(DeliverNote deliverNote) {
       long total = 0;
       if (deliverNote.getRows() != null && deliverNote.getRows() > 0) {
           total = this.count(deliverNote);
       }
       return PagerResultObject.of(deliverNote, total,
              this.deliverNoteMapper.pageDeliverNotes(deliverNote));
    }
    public void createNewDeliverNote(DeliverNote deliverNote){

        deliverNote.setDeliverStatus(DeliveryStatus.CREATE);
        deliverNote.setCreateTime(new Date());
        deliverNoteMapper.create(deliverNote);

        //记录配送状态流水
        DeliverFlow deliverFlow = new DeliverFlow();
        deliverFlow.setCreateAt(new Date());
        deliverFlow.setDeliverNoteId(deliverNote.getId());
        deliverFlow.setPreStatus(null);
        deliverFlow.setStatus(deliverNote.getDeliverStatus());
        deliverFlowMapper.create(deliverFlow);
    }

}

