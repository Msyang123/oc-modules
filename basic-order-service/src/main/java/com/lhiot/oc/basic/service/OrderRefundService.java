package com.lhiot.oc.basic.service;

import com.lhiot.oc.basic.domain.*;
import com.lhiot.oc.basic.domain.common.PagerResultObject;
import com.lhiot.oc.basic.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
* 描述：服务类
* @author yijun
* @date 2018-07-21
*/
@Service
@Transactional
public class OrderRefundService {

    private final OrderRefundMapper orderRefundMapper;

    @Autowired
    public OrderRefundService(OrderRefundMapper orderRefundMapper) {
        this.orderRefundMapper = orderRefundMapper;
    }

    public int create(OrderRefund orderRefund){
        return this.orderRefundMapper.create(orderRefund);
    }

    public int updateById(OrderRefund orderRefund){
        return this.orderRefundMapper.updateById(orderRefund);
    }

    public int deleteByIds(String ids){
        List<String> idList = Arrays.asList(ids.split(","));
        return this.orderRefundMapper.deleteByIds(idList);
    }

    public List<OrderRefund> list(OrderRefund orderRefund){
        return this.orderRefundMapper.list(orderRefund);
    }

    public OrderRefund findById(Long id){
        return this.orderRefundMapper.findById(id);
    }


    public long count(OrderRefund orderRefund){
        return this.orderRefundMapper.pageQueryCount(orderRefund);
    }
    
    public PagerResultObject<OrderRefund> pageList(OrderRefund orderRefund) {
        long total = 0;
        if (orderRefund.getRows() != null && orderRefund.getRows() > 0) {
            total = this.count(orderRefund);
        }
        return PagerResultObject.of(orderRefund, total,
                this.orderRefundMapper.page(orderRefund));
    }

}

