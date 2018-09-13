package com.lhiot.oc.basic.service;

import com.lhiot.oc.basic.domain.DeliverNote;
import com.lhiot.oc.basic.domain.common.PagerResultObject;
import com.lhiot.oc.basic.mapper.DeliverNoteMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
* Description:配送单信息服务类
* @author zhangshu
* @date 2018/08/06
*/
@Service
@Transactional
public class DeliveryNoteService {

    private final DeliverNoteMapper deliverNoteMapper;

    @Autowired
    public DeliveryNoteService(DeliverNoteMapper deliverNoteMapper) {
        this.deliverNoteMapper = deliverNoteMapper;
    }

    /** 
    * Description:根据id修改配送单信息
    *  
    * @param deliverNote
    * @return
    * @author zhangshu
    * @date 2018/08/06 09:10:22
    */ 
    public int updateById(DeliverNote deliverNote){
        return this.deliverNoteMapper.updateById(deliverNote);
    }

    /** 
    * Description:根据ids删除配送单信息
    *  
    * @param ids
    * @return
    * @author zhangshu
    * @date 2018/08/06 09:10:22
    */ 
    public int deleteByIds(String ids){
        return this.deliverNoteMapper.deleteByIds(Arrays.asList(ids.split(",")));
    }
    
    /** 
    * Description:根据id查找配送单信息
    *  
    * @param id
    * @return
    * @author zhangshu
    * @date 2018/08/06 09:10:22
    */ 
    public DeliverNote selectById(Long id){
        return this.deliverNoteMapper.selectById(id);
    }
    /**
     * Description:根据id查找配送单信息
     *
     * @param orderId
     * @return
     * @author zhangshu
     * @date 2018/08/06 09:10:22
     */
    public List<DeliverNote> selectByOrderId(Long orderId){
        return this.deliverNoteMapper.selectByOrderId(orderId);
    }

    /**
     * Description:根据orderId查找最新配送单信息
     *
     * @param orderId
     * @return
     * @author zhangshu
     * @date 2018/08/06 09:10:22
     */
    public DeliverNote selectLastByOrderId(Long orderId){
        return this.deliverNoteMapper.selectLastByOrderId(orderId);
    }

    /** 
    * Description: 查询配送单信息总记录数
    *  
    * @param deliverNote
    * @return
    * @author zhangshu
    * @date 2018/08/06 09:10:22
    */  
    public long count(DeliverNote deliverNote){
        return this.deliverNoteMapper.pageDeliverNoteCounts(deliverNote);
    }
    
    /** 
    * Description: 查询配送单信息分页列表
    *  
    * @param deliverNote
    * @return
    * @author zhangshu
    * @date 2018/08/06 09:10:22
    */  
    public PagerResultObject<DeliverNote> pageList(DeliverNote deliverNote) {
       long total = 0;
       if (deliverNote.getRows() != null && deliverNote.getRows() > 0) {
           total = this.count(deliverNote);
       }
       return PagerResultObject.of(deliverNote, total,
              this.deliverNoteMapper.pageDeliverNotes(deliverNote));
    }
    public void createNewDeliverNote(DeliverNote deliverNote,DeliverNote.DeliverType deliverType){
/*        DeliverNote deliverNote = new DeliverNote();
        deliverNote.setOrderId(baseOrderInfo.getId());
        deliverNote.setOrderCode(baseOrderInfo.getHdOrderCode());
        deliverNote.setDeliverType(deliverType);
        deliverNote.setStoreCode(baseOrderInfo.getStoreCode());
        deliverNote.setRemark(baseOrderInfo.getRemark());
        deliverNote.setDeliverStatus(DeliverNote.DeliveryStatus.UNRECEIVE);
        deliverNote.setFee(baseOrderInfo.getDeliveryFee());*/

        deliverNote.setDeliverStatus(DeliverNote.DeliveryStatus.UNRECEIVE);
        deliverNoteMapper.create(deliverNote);
    }

}

