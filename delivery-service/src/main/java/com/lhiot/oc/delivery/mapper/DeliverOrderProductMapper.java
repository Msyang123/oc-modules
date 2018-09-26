package com.lhiot.oc.delivery.mapper;

import com.lhiot.oc.delivery.domain.DeliverOrderProduct;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
* Description:配送订单商品列Mapper类
* @author yijun 2018/09/18 created
*/
@Mapper
@Repository
public interface DeliverOrderProductMapper {

    /**
    * Description:批量新增配送订单商品列
    */
    int createInBatch(List<DeliverOrderProduct> deliverOrderProductList);

    /**
    * Description:根据id修改配送订单商品列
    */
    int updateById(DeliverOrderProduct deliverOrderProduct);

    /**
    * Description:根据ids删除配送订单商品列
    */
    int deleteByIds(java.util.List<String> ids);

    /**
    * Description:根据id查找配送订单商品列
    */
    DeliverOrderProduct selectById(Long id);

    /**
    * Description:查询配送订单商品列列表
    */
     List<DeliverOrderProduct> pageDeliverOrderProducts(DeliverOrderProduct deliverOrderProduct);


    /**
    * Description: 查询配送订单商品列总记录数
    */
    long pageDeliverOrderProductCounts(DeliverOrderProduct deliverOrderProduct);
}
