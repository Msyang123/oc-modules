package com.lhiot.oc.delivery.repository;

import com.lhiot.oc.delivery.model.DeliverOrder;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
* Description:配送订单流程Mapper类
* @author yijun 2018/09/16 created
*/
@Mapper
@Repository
public interface DeliverBaseOrderMapper {

    /**
    * Description:新增配送订单流程
    */
    int create(DeliverOrder deliverBaseOrder);

    /**
    * Description:根据id修改配送订单流程
    */
    int updateById(DeliverOrder deliverBaseOrder);

    /**
    * Description:根据ids删除配送订单流程
    */
    int deleteByIds(List<String> ids);

    /**
    * Description:根据id查找配送订单流程
    */
    DeliverOrder selectById(Long id);

    /**
     * Description:根据海鼎code查找配送订单流程
     */
    DeliverOrder selectByHdOrderCode(String hdOrderCode);

    /**
    * Description:查询配送订单流程列表
    */
     List<DeliverOrder> pageDeliverBaseOrders(DeliverOrder deliverBaseOrder);


    /**
    * Description: 查询配送订单流程总记录数
    */
    long pageDeliverBaseOrderCounts(DeliverOrder deliverBaseOrder);
}
