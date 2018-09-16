package com.lhiot.oc.basic.mapper;

import com.lhiot.oc.basic.domain.DeliverNote;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
* Description:配送单信息Mapper类
* @author zhangshu
* @date 2018/08/06
*/
@Mapper
public interface DeliverNoteMapper {

    /**
    * Description:新增配送单信息
    *
    * @param deliverNote
    * @return
    * @author zhangshu
    * @date 2018/08/06 09:10:22
    */
    int create(DeliverNote deliverNote);

    /**
    * Description:根据id修改配送单信息
    *
    * @param deliverNote
    * @return
    * @author zhangshu
    * @date 2018/08/06 09:10:22
    */
    int updateById(DeliverNote deliverNote);

    /**
    * Description:根据ids删除配送单信息
    *
    * @param ids
    * @return
    * @author zhangshu
    * @date 2018/08/06 09:10:22
    */
    int deleteByIds(List<String> ids);

    /**
    * Description:根据id查找配送单信息
    *
    * @param id
    * @return
    * @author zhangshu
    * @date 2018/08/06 09:10:22
    */
    DeliverNote selectById(Long id);

    /**
     * 依据配送单编码查询
     * @param deliverCode
     * @return
     */
    DeliverNote selectByDeliverCode(String deliverCode);

    /**
    * Description:查询配送单信息列表
    *
    * @param deliverNote
    * @return
    * @author zhangshu
    * @date 2018/08/06 09:10:22
    */
     List<DeliverNote> pageDeliverNotes(DeliverNote deliverNote);


    /**
    * Description: 查询配送单信息总记录数
    *
    * @param deliverNote
    * @return
    * @author zhangshu
    * @date 2018/08/06 09:10:22
    */
    long pageDeliverNoteCounts(DeliverNote deliverNote);

    List<DeliverNote> selectByOrderId(Long id);


    DeliverNote selectLastByOrderId(Long orderId);
}
