package com.lhiot.oc.delivery.mapper;

import com.lhiot.oc.delivery.domain.DeliverOrderProduct;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
* Description:配送订单商品列Mapper类
* @author yijun
* @date 2018/09/18
*/
@Mapper
public interface DeliverOrderProductMapper {

    /**
    * Description:批量新增配送订单商品列
    *
    * @param deliverOrderProductList
    * @return
    * @author yijun
    * @date 2018/09/18 09:41:12
    */
    int createInBatch(List<DeliverOrderProduct> deliverOrderProductList);

    /**
    * Description:根据id修改配送订单商品列
    *
    * @param deliverOrderProduct
    * @return
    * @author yijun
    * @date 2018/09/18 09:41:12
    */
    int updateById(DeliverOrderProduct deliverOrderProduct);

    /**
    * Description:根据ids删除配送订单商品列
    *
    * @param ids
    * @return
    * @author yijun
    * @date 2018/09/18 09:41:12
    */
    int deleteByIds(java.util.List<String> ids);

    /**
    * Description:根据id查找配送订单商品列
    *
    * @param id
    * @return
    * @author yijun
    * @date 2018/09/18 09:41:12
    */
    DeliverOrderProduct selectById(Long id);

    /**
    * Description:查询配送订单商品列列表
    *
    * @param deliverOrderProduct
    * @return
    * @author yijun
    * @date 2018/09/18 09:41:12
    */
     List<DeliverOrderProduct> pageDeliverOrderProducts(DeliverOrderProduct deliverOrderProduct);


    /**
    * Description: 查询配送订单商品列总记录数
    *
    * @param deliverOrderProduct
    * @return
    * @author yijun
    * @date 2018/09/18 09:41:12
    */
    long pageDeliverOrderProductCounts(DeliverOrderProduct deliverOrderProduct);
}
