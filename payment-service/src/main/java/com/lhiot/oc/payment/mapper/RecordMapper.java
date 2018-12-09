package com.lhiot.oc.payment.mapper;

import com.lhiot.oc.payment.entity.Record;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Mapper
@Repository
public interface RecordMapper {

    int insert(Record record);

    Record one(Long outTradeNo);

    List<Record> selectList(Map<String,Object> map);

    List<Record> selectPages(Map<String, Object> param);

    int count(Map<String, Object> param);

    int completed(Map<String,Object> map);
}
