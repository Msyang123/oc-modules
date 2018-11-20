package com.lhiot.oc.delivery.repository;

import com.lhiot.oc.delivery.entity.DeliverNote;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
* Description:配送单信息Mapper类
* @author zhangshu 2018/08/06 created
*/
@Mapper
@Repository
public interface DeliverNoteMapper {

    /**
    * Description:新增配送单信息
    */
    int create(DeliverNote deliverNote);

    /**
    * Description:根据id修改配送单信息
    */
    int updateById(DeliverNote deliverNote);

    /**
    * Description:根据ids删除配送单信息
    */
    int deleteByIds(List<String> ids);

    /**
    * Description:根据id查找配送单信息
    */
    DeliverNote selectById(Long id);

    /**
     * 依据配送单编码查询
     */
    DeliverNote selectByDeliverCode(String deliverCode);

    /**
    * Description:查询配送单信息列表
    */
     List<DeliverNote> pageDeliverNotes(DeliverNote deliverNote);


    /**
    * Description: 查询配送单信息总记录数
    */
    long pageDeliverNoteCounts(DeliverNote deliverNote);

    List<DeliverNote> selectByOrderId(Long id);


    DeliverNote selectLastByOrderId(Long orderId);
}
