package com.lhiot.oc.delivery.mapper;

import com.lhiot.oc.delivery.domain.DeliverBaseOrder;
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
    int create(DeliverBaseOrder deliverBaseOrder);

    /**
    * Description:根据id修改配送订单流程
    */
    int updateById(DeliverBaseOrder deliverBaseOrder);

    /**
    * Description:根据ids删除配送订单流程
    */
    int deleteByIds(List<String> ids);

    /**
    * Description:根据id查找配送订单流程
    */
    DeliverBaseOrder selectById(Long id);

    /**
     * Description:根据海鼎code查找配送订单流程
     */
    DeliverBaseOrder selectByHdOrderCode(String hdOrderCode);

    /**
    * Description:查询配送订单流程列表
    */
     List<DeliverBaseOrder> pageDeliverBaseOrders(DeliverBaseOrder deliverBaseOrder);


    /**
    * Description: 查询配送订单流程总记录数
    */
    long pageDeliverBaseOrderCounts(DeliverBaseOrder deliverBaseOrder);
}
