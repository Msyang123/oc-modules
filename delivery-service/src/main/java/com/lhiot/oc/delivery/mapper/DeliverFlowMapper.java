package com.lhiot.oc.delivery.mapper;

import com.lhiot.oc.delivery.domain.DeliverFlow;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
* Description:配送状态流转记录Mapper类
* @author yijun
* @date 2018/09/16
*/
@Mapper
@Repository
public interface DeliverFlowMapper {

    /**
    * Description:新增配送状态流转记录
    *
    * @param deliverFlow
    * @return
    * @author yijun
    * @date 2018/09/16 10:36:35
    */
    int create(DeliverFlow deliverFlow);

    /**
    * Description:根据id修改配送状态流转记录
    *
    * @param deliverFlow
    * @return
    * @author yijun
    * @date 2018/09/16 10:36:35
    */
    int updateById(DeliverFlow deliverFlow);

    /**
    * Description:根据ids删除配送状态流转记录
    *
    * @param ids
    * @return
    * @author yijun
    * @date 2018/09/16 10:36:35
    */
    int deleteByIds(java.util.List<String> ids);

    /**
    * Description:根据id查找配送状态流转记录
    *
    * @param id
    * @return
    * @author yijun
    * @date 2018/09/16 10:36:35
    */
    DeliverFlow selectById(Long id);

    /**
    * Description:查询配送状态流转记录列表
    *
    * @param deliverFlow
    * @return
    * @author yijun
    * @date 2018/09/16 10:36:35
    */
     List<DeliverFlow> pageDeliverFlows(DeliverFlow deliverFlow);


    /**
    * Description: 查询配送状态流转记录总记录数
    *
    * @param deliverFlow
    * @return
    * @author yijun
    * @date 2018/09/16 10:36:35
    */
    long pageDeliverFlowCounts(DeliverFlow deliverFlow);
}
