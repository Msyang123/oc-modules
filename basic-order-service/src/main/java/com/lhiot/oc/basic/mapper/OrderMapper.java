package com.lhiot.oc.basic.mapper;

import com.lhiot.oc.basic.domain.BaseOrderInfo;
import com.lhiot.oc.basic.domain.inparam.QueryParam;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OrderMapper {
	
	int create(BaseOrderInfo baseOrderInfo);

	/**
	 * 更新订单信息，除订单状态
	 * @param baseOrderInfo
	 * @return
	 */
	int update(BaseOrderInfo baseOrderInfo);

	/**
	 * 依据订单id修改订单状态
	 * @param baseOrderInfo
	 * @return
	 */
	int updateOrderStatusById(BaseOrderInfo baseOrderInfo);

	/**
	 * 依据订单code修改订单状态
	 * @param baseOrderInfo
	 * @return
	 */
	int updateOrderStatusByCode(BaseOrderInfo baseOrderInfo);


	BaseOrderInfo findByHdCode(String hdCode);

	BaseOrderInfo findByCode(String code);
	
	BaseOrderInfo findById(Long id);


	Long orderQueryCounts(QueryParam queryParam);

	List<BaseOrderInfo> orderQuery(QueryParam queryParam);

}
