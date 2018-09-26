package com.lhiot.oc.delivery.service;

import com.lhiot.oc.delivery.domain.DeliverBaseOrder;
import com.lhiot.oc.delivery.domain.common.PagerResultObject;
import com.lhiot.oc.delivery.mapper.DeliverBaseOrderMapper;
import com.lhiot.oc.delivery.mapper.DeliverOrderProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Objects;

/**
* Description:配送订单流程服务类
* @author yijun date 2018/09/16
*/
@Service
@Transactional
public class DeliverBaseOrderService {

    private final DeliverBaseOrderMapper deliverBaseOrderMapper;

    private final DeliverOrderProductMapper deliverOrderProductMapper;

    @Autowired
    public DeliverBaseOrderService(DeliverBaseOrderMapper deliverBaseOrderMapper, DeliverOrderProductMapper deliverOrderProductMapper) {
        this.deliverBaseOrderMapper = deliverBaseOrderMapper;
        this.deliverOrderProductMapper = deliverOrderProductMapper;
    }

    /** 
    * Description:新增配送订单流程
    */  
    public int create(DeliverBaseOrder deliverBaseOrder){
        int i = 0;
        //如果查询到，就不新增
        if(Objects.isNull(this.deliverBaseOrderMapper.selectByHdOrderCode(deliverBaseOrder.getHdOrderCode()))){
            this.deliverBaseOrderMapper.create(deliverBaseOrder);
            //给配送订单商品设置配送订单id
            deliverBaseOrder.getDeliverOrderProductList().forEach(item->item.setDeliverBaseOrderId(deliverBaseOrder.getId()));
            i = deliverOrderProductMapper.createInBatch(deliverBaseOrder.getDeliverOrderProductList());
        }
        return i;
    }

    /** 
    * Description:根据id修改配送订单流程
    *  
    * @param deliverBaseOrder DeliverBaseOrder
    * @return int
    */ 
    public int updateById(DeliverBaseOrder deliverBaseOrder){
        return this.deliverBaseOrderMapper.updateById(deliverBaseOrder);
    }

    /** 
    * Description:根据ids删除配送订单流程
    *  
    * @param ids ids
    * @return int
    */ 
    public int deleteByIds(String ids){
        return this.deliverBaseOrderMapper.deleteByIds(Arrays.asList(ids.split(",")));
    }
    
    /** 
    * Description:根据id查找配送订单流程
    */ 
    public DeliverBaseOrder selectById(Long id){
        return this.deliverBaseOrderMapper.selectById(id);
    }

    /** 
    * Description: 查询配送订单流程总记录数
    */  
    public long count(DeliverBaseOrder deliverBaseOrder){
        return this.deliverBaseOrderMapper.pageDeliverBaseOrderCounts(deliverBaseOrder);
    }
    
    /** 
    * Description: 查询配送订单流程分页列表
    */  
    public PagerResultObject<DeliverBaseOrder> pageList(DeliverBaseOrder deliverBaseOrder) {
       long total = 0;
       if (deliverBaseOrder.getRows() != null && deliverBaseOrder.getRows() > 0) {
           total = this.count(deliverBaseOrder);
       }
       return PagerResultObject.of(deliverBaseOrder, total,
              this.deliverBaseOrderMapper.pageDeliverBaseOrders(deliverBaseOrder));
    }
}

