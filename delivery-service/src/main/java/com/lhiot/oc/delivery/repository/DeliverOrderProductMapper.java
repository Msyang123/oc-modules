package com.lhiot.oc.delivery.repository;

import com.lhiot.oc.delivery.model.DeliverOrder;
import com.lhiot.oc.delivery.model.DeliverProduct;
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
    int createInBatch(List<DeliverProduct> deliverOrderProductList);

    /**
    * Description:根据id修改配送订单商品列
    */
    int updateById(DeliverProduct deliverOrderProduct);

    /**
    * Description:根据ids删除配送订单商品列
    */
    int deleteByIds(List<String> ids);

    /**
    * Description:根据id查找配送订单商品列
    */
    DeliverOrder selectById(Long id);

    /**
    * Description:查询配送订单商品列列表
    */
     List<DeliverProduct> pageDeliverOrderProducts(DeliverProduct deliverOrderProduct);


    /**
    * Description: 查询配送订单商品列总记录数
    */
    long pageDeliverOrderProductCounts(DeliverProduct deliverOrderProduct);
}
