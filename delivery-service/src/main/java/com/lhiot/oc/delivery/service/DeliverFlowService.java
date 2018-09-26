package com.lhiot.oc.delivery.service;

import com.lhiot.oc.delivery.domain.DeliverFlow;
import com.lhiot.oc.delivery.domain.common.PagerResultObject;
import com.lhiot.oc.delivery.mapper.DeliverFlowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

/**
 * Description:配送状态流转记录服务类
 *
 * @author yijun date 2018/09/16
 */
@Service
@Transactional
public class DeliverFlowService {

    private final DeliverFlowMapper deliverFlowMapper;

    @Autowired
    public DeliverFlowService(DeliverFlowMapper deliverFlowMapper) {
        this.deliverFlowMapper = deliverFlowMapper;
    }

    /**
     * Description:新增配送状态流转记录
     */
    public int create(DeliverFlow deliverFlow) {
        return this.deliverFlowMapper.create(deliverFlow);
    }

    /**
     * Description:根据id修改配送状态流转记录
     */
    public int updateById(DeliverFlow deliverFlow) {
        return this.deliverFlowMapper.updateById(deliverFlow);
    }

    /**
     * Description:根据ids删除配送状态流转记录
     */
    public int deleteByIds(String ids) {
        return this.deliverFlowMapper.deleteByIds(Arrays.asList(ids.split(",")));
    }

    /**
     * Description:根据id查找配送状态流转记录
     */
    public DeliverFlow selectById(Long id) {
        return this.deliverFlowMapper.selectById(id);
    }

    /**
     * Description: 查询配送状态流转记录总记录数
     */
    public long count(DeliverFlow deliverFlow) {
        return this.deliverFlowMapper.pageDeliverFlowCounts(deliverFlow);
    }

    /**
     * Description: 查询配送状态流转记录分页列表
     */
    public PagerResultObject<DeliverFlow> pageList(DeliverFlow deliverFlow) {
        long total = 0;
        if (deliverFlow.getRows() != null && deliverFlow.getRows() > 0) {
            total = this.count(deliverFlow);
        }
        return PagerResultObject.of(deliverFlow, total,
                this.deliverFlowMapper.pageDeliverFlows(deliverFlow));
    }
}

