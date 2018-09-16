package com.lhiot.oc.basic.service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.lhiot.oc.basic.domain.DeliverBaseOrder;
import com.lhiot.oc.basic.domain.common.PagerResultObject;
import com.lhiot.oc.basic.mapper.DeliverBaseOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
* Description:配送订单流程服务类
* @author yijun
* @date 2018/09/16
*/
@Service
@Transactional
public class DeliverBaseOrderService {

    private final DeliverBaseOrderMapper deliverBaseOrderMapper;

    @Autowired
    public DeliverBaseOrderService(DeliverBaseOrderMapper deliverBaseOrderMapper) {
        this.deliverBaseOrderMapper = deliverBaseOrderMapper;
    }

    /** 
    * Description:新增配送订单流程
    *  
    * @param deliverBaseOrder
    * @return
    * @author yijun
    * @date 2018/09/16 10:23:37
    */  
    public int create(DeliverBaseOrder deliverBaseOrder){
        //如果查询到，就不新增
        if(Objects.isNull(this.deliverBaseOrderMapper.selectByHdOrderCode(deliverBaseOrder.getHdOrderCode()))){
            this.deliverBaseOrderMapper.create(deliverBaseOrder);
        }
        return 1;
    }

    /** 
    * Description:根据id修改配送订单流程
    *  
    * @param deliverBaseOrder
    * @return
    * @author yijun
    * @date 2018/09/16 10:23:37
    */ 
    public int updateById(DeliverBaseOrder deliverBaseOrder){
        return this.deliverBaseOrderMapper.updateById(deliverBaseOrder);
    }

    /** 
    * Description:根据ids删除配送订单流程
    *  
    * @param ids
    * @return
    * @author yijun
    * @date 2018/09/16 10:23:37
    */ 
    public int deleteByIds(String ids){
        return this.deliverBaseOrderMapper.deleteByIds(Arrays.asList(ids.split(",")));
    }
    
    /** 
    * Description:根据id查找配送订单流程
    *  
    * @param id
    * @return
    * @author yijun
    * @date 2018/09/16 10:23:37
    */ 
    public DeliverBaseOrder selectById(Long id){
        return this.deliverBaseOrderMapper.selectById(id);
    }

    /** 
    * Description: 查询配送订单流程总记录数
    *  
    * @param deliverBaseOrder
    * @return
    * @author yijun
    * @date 2018/09/16 10:23:37
    */  
    public long count(DeliverBaseOrder deliverBaseOrder){
        return this.deliverBaseOrderMapper.pageDeliverBaseOrderCounts(deliverBaseOrder);
    }
    
    /** 
    * Description: 查询配送订单流程分页列表
    *  
    * @param deliverBaseOrder
    * @return
    * @author yijun
    * @date 2018/09/16 10:23:37
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

