package com.lhiot.oc.basic.mapper;

import com.lhiot.oc.basic.domain.DeliverBaseOrder;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
* Description:配送订单流程Mapper类
* @author yijun
* @date 2018/09/16
*/
@Mapper
public interface DeliverBaseOrderMapper {

    /**
    * Description:新增配送订单流程
    *
    * @param deliverBaseOrder
    * @return
    * @author yijun
    * @date 2018/09/16 10:23:37
    */
    int create(DeliverBaseOrder deliverBaseOrder);

    /**
    * Description:根据id修改配送订单流程
    *
    * @param deliverBaseOrder
    * @return
    * @author yijun
    * @date 2018/09/16 10:23:37
    */
    int updateById(DeliverBaseOrder deliverBaseOrder);

    /**
    * Description:根据ids删除配送订单流程
    *
    * @param ids
    * @return
    * @author yijun
    * @date 2018/09/16 10:23:37
    */
    int deleteByIds(List<String> ids);

    /**
    * Description:根据id查找配送订单流程
    *
    * @param id
    * @return
    * @author yijun
    * @date 2018/09/16 10:23:37
    */
    DeliverBaseOrder selectById(Long id);

    /**
     * Description:根据海鼎code查找配送订单流程
     *
     * @param hdOrderCode
     * @return
     * @author yijun
     * @date 2018/09/16 10:23:37
     */
    DeliverBaseOrder selectByHdOrderCode(String hdOrderCode);

    /**
    * Description:查询配送订单流程列表
    *
    * @param deliverBaseOrder
    * @return
    * @author yijun
    * @date 2018/09/16 10:23:37
    */
     List<DeliverBaseOrder> pageDeliverBaseOrders(DeliverBaseOrder deliverBaseOrder);


    /**
    * Description: 查询配送订单流程总记录数
    *
    * @param deliverBaseOrder
    * @return
    * @author yijun
    * @date 2018/09/16 10:23:37
    */
    long pageDeliverBaseOrderCounts(DeliverBaseOrder deliverBaseOrder);
}
