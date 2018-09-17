package com.lhiot.oc.basic.service;

import com.leon.microx.support.result.Tips;
import com.leon.microx.util.Calculator;
import com.leon.microx.util.Jackson;
import com.lhiot.oc.basic.domain.DeliverBaseOrder;
import com.lhiot.oc.basic.domain.DeliverNote;
import com.lhiot.oc.basic.domain.enums.DeliverNeedConver;
import com.lhiot.oc.basic.domain.enums.DeliverType;
import com.lhiot.oc.basic.domain.enums.DeliveryStatus;
import com.lhiot.oc.basic.feign.BaseDataServiceFeign;
import com.lhiot.oc.basic.feign.BaseOrderServiceFeign;
import com.lhiot.oc.basic.feign.ThirdPartyServiceFeign;
import com.lhiot.oc.basic.feign.domain.*;
import com.lhiot.oc.basic.util.Distance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@Transactional
public class FengniaoDeliveryService implements IDelivery{

	private final DeliveryNoteService deliveryNoteService;
	private final ThirdPartyServiceFeign thirdPartyServiceFeign;
	private final BaseOrderServiceFeign baseOrderServiceFeign;
	private final BaseDataServiceFeign baseDataServiceFeign;
	private final DeliverBaseOrderService deliverBaseOrderService;

	public FengniaoDeliveryService(DeliveryNoteService deliveryNoteService, ThirdPartyServiceFeign thirdPartyServiceFeign, BaseOrderServiceFeign baseOrderServiceFeign, BaseDataServiceFeign baseDataServiceFeign, DeliverBaseOrderService deliverBaseOrderService){
		this.thirdPartyServiceFeign = thirdPartyServiceFeign;
		this.baseOrderServiceFeign = baseOrderServiceFeign;
        this.deliveryNoteService = deliveryNoteService;
		this.baseDataServiceFeign = baseDataServiceFeign;
		this.deliverBaseOrderService = deliverBaseOrderService;
	}

	/**
	 * 发送订单给蜂鸟
	 * @return
	 * @throws Exception
	 */
	public Tips send(DeliverNeedConver deliverNeedConver, DeliverBaseOrder deliverBaseOrder){
		log.info("发送蜂鸟的订单{}",deliverBaseOrder);

		ElemeCreateOrderRequest.ElemeCreateRequestData elemeCreateRequestData = new ElemeCreateOrderRequest.ElemeCreateRequestData();

		ResponseEntity<Store> storeResponseEntity = baseDataServiceFeign.findStoreByCode(deliverBaseOrder.getStoreCode(),deliverBaseOrder.getApplyType());
		if(Objects.isNull(storeResponseEntity)||storeResponseEntity.getStatusCode().isError()){
			return Tips.of(-1,"远程查询门店信息失败");
		}
		//获取到门店基础信息
		Store store = storeResponseEntity.getBody();
		//距离换算
		BigDecimal distance = Distance.getDistance(store.getStorePosition().getLat(),store.getStorePosition().getLng(),
				deliverBaseOrder.getLat(),deliverBaseOrder.getLng());

		//设置门店编码
		elemeCreateRequestData.setChainStoreCode(store.getStoreCode());
		//配送地址信息
		ElemeCreateOrderRequest.TransportInfo transportInfo = new ElemeCreateOrderRequest.TransportInfo();
		transportInfo.setAddress(store.getStoreAddress());
		transportInfo.setLatitude(store.getStorePosition().getLat());
		transportInfo.setLongitude(store.getStorePosition().getLng());
		transportInfo.setName(store.getStoreName());
		transportInfo.setRemark("");
		transportInfo.setTel(store.getStorePhone());
		if(Objects.equals(deliverNeedConver,DeliverNeedConver.YES)){
			transportInfo.setPositionSource(2);//百度
		}else{
			transportInfo.setPositionSource(3);//高德和腾讯使用相同坐标标准
		}
		elemeCreateRequestData.setTransportInfo(transportInfo);

		//收货人
		ElemeCreateOrderRequest.ReceiverInfo receiverInfo = new ElemeCreateOrderRequest.ReceiverInfo();
		receiverInfo.setAddress(deliverBaseOrder.getAddress());
		receiverInfo.setCityCode("0731");
		receiverInfo.setCityName("长沙市");
		receiverInfo.setLatitude(new BigDecimal(deliverBaseOrder.getLat()));
		receiverInfo.setLongitude(new BigDecimal(deliverBaseOrder.getLng()));
		receiverInfo.setName(deliverBaseOrder.getReceiveUser());
		//如果转换 门店位置和收货人位置都转换
		if(Objects.equals(deliverNeedConver,DeliverNeedConver.YES)){
			receiverInfo.setPositionSource(2);//百度
		}else{
			receiverInfo.setPositionSource(3);//高德和腾讯使用相同坐标标准
		}
		receiverInfo.setPrimaryPhone(deliverBaseOrder.getContactPhone());
		elemeCreateRequestData.setReceiverInfo(receiverInfo);

		elemeCreateRequestData.setGoodsCount(deliverBaseOrder.getGoodsCount());//默认一个
		elemeCreateRequestData.setIfNeedAgentPayment(0);//不需要代购
		elemeCreateRequestData.setIfNeedInvoiced(0);//不需要发票
		//此处不设置商品列表
		// elemeCreateRequestData.setItemsJson();

		//elemeCreateRequestData.setNotifyUrl("http://172.16.10.203:8211/thirdparty-service-v1-0/delivery/fengniao/callback");
		elemeCreateRequestData.setOrderActualAmount(new BigDecimal(Calculator.div(deliverBaseOrder.getAmountPayable(),100.0)));//应付订单金额
		elemeCreateRequestData.setOrderAddTime(System.currentTimeMillis());
		elemeCreateRequestData.setOrderPaymentMethod(1);
		elemeCreateRequestData.setOrderRemark(deliverBaseOrder.getRemark());
		elemeCreateRequestData.setOrderPaymentStatus(1);//已支付
		elemeCreateRequestData.setOrderTotalAmount(new BigDecimal(Calculator.div(deliverBaseOrder.getTotalAmount(),100.0)));//订单总金额
		elemeCreateRequestData.setOrderType(1);
		elemeCreateRequestData.setOrderWeight(new BigDecimal(deliverBaseOrder.getCargoWeight()));
		elemeCreateRequestData.setPartnerOrderCode(deliverBaseOrder.getHdOrderCode());

		ResponseEntity<String> responseEntity = thirdPartyServiceFeign.addOrder(elemeCreateRequestData);
		if(Objects.isNull(responseEntity) || responseEntity.getStatusCode().isError()){
			return Tips.of(-1,"发送蜂鸟订单失败");
		}
		//发送蜂鸟返回结果
		FengniaoOrderAddResult fengniaoOrderAddResult = Jackson.object(responseEntity.getBody(), FengniaoOrderAddResult.class);
		log.info("蜂鸟配送返回结果{}",fengniaoOrderAddResult);
		if(fengniaoOrderAddResult.getCode()==200){
			//记录配送信息
			DeliverNote deliverNote = new DeliverNote();
			deliverNote.setOrderId(deliverBaseOrder.getId());
			deliverNote.setOrderCode(deliverBaseOrder.getOrderCode());//订单号
			deliverNote.setDeliverCode(deliverBaseOrder.getHdOrderCode());//配送单号与海鼎订单号一致
			deliverNote.setDeliverType(DeliverType.FENGNIAO);
			deliverNote.setStoreCode(deliverBaseOrder.getStoreCode());
			deliverNote.setRemark(deliverBaseOrder.getRemark());
			deliverNote.setFee(deliverBaseOrder.getDeliveryFee());//自己传递过来的配送费
			deliverNote.setDistance(distance.doubleValue());//自己计算的直线距离
			deliverNote.setDeliverCode(deliverBaseOrder.getHdOrderCode());//配送单单号
			//创建配送单
			deliveryNoteService.createNewDeliverNote(deliverNote);
			//写入配送订单流程表
			deliverBaseOrderService.create(deliverBaseOrder);
			return Tips.of(1,"创建蜂鸟配送成功");
		}

		return Tips.of(-1,"创建蜂鸟配送失败");
	}


	/**
	 * 取消配送订单原因列表
	 * @return
	 */
	public String cancelOrderReasons(){
		return thirdPartyServiceFeign.cancelFengniaoOrderReasons().getBody();
	}

	/**
	 * @param hdOrderCode      订单hdCode
	 * @param cancelReasonId 取消原因id
	 * @param cancelReason   取消原因说明
	 * @Description: 取消蜂鸟订单
	 * @return: java.lang.String
	 * @Author: Limiaojun
	 * @Date: 2018/7/19
	 */
	public Tips cancel(String hdOrderCode, int cancelReasonId, String cancelReason){
		DeliverNote searchDeliverNote = deliveryNoteService.selectByDeliverCode(hdOrderCode);
		if(Objects.isNull(searchDeliverNote)){
			return Tips.of(-1,"未找到配送单信息");
		}

		ElemeCancelOrderRequest.ElemeCancelOrderRequstData elemeCancelOrderRequstData=new ElemeCancelOrderRequest.ElemeCancelOrderRequstData();
		elemeCancelOrderRequstData.setPartnerOrderCode(hdOrderCode);
		elemeCancelOrderRequstData.setOrderCancelReasonCode(2);
		elemeCancelOrderRequstData.setOrderCancelCode(cancelReasonId);
		elemeCancelOrderRequstData.setOrderCancelDescription(cancelReason);
		elemeCancelOrderRequstData.setOrderCancelTime(System.currentTimeMillis());
		//elemeCancelOrderRequstData.setOrderCancelNotifyUrl("http://172.16.10.203:8211/thirdparty-service-v1-0/delivery/fengniao/callback");

		ResponseEntity<String> cancelResponseEntity = thirdPartyServiceFeign.cancel(elemeCancelOrderRequstData);
		if(Objects.isNull(cancelResponseEntity)||cancelResponseEntity.getStatusCode().isError()){
			return Tips.of(-1,"取消蜂鸟配送失败");
		}
		//修改数据库中记录
		searchDeliverNote.setFailureCause(cancelReason);
		searchDeliverNote.setCancelTime(new Date());
		searchDeliverNote.setDeliverStatus(DeliveryStatus.FAILURE);
		deliveryNoteService.updateById(searchDeliverNote);
		return Tips.of(1,"取消蜂鸟配送成功");
	}

	/**
	 * 蜂鸟回调处理
	 * @param backMsg
	 * @return
	 */
	public Tips callBack(String backMsg) {
		Map<String,Object> backMap = Jackson.map(backMsg);
		String dataBody = null;
		try {
			dataBody = URLDecoder.decode(backMap.get("data").toString(), "utf-8");
		} catch (UnsupportedEncodingException e) {
			log.error("不支持的字符转换格式");
		}
		Map<String,Object> backDataMap = Jackson.map(dataBody);
		// 返回的订单编码
		String deliverCode = backDataMap.get("partner_order_code").toString();

		DeliverNote deliverNote = deliveryNoteService.selectByDeliverCode(deliverCode);

		if(Objects.isNull(deliverNote)){
			log.error("未找到蜂鸟配送回调订单{}" + deliverCode);
			return Tips.of(-1,"未找到蜂鸟配送回调订单"+deliverCode);
		}
		// 修改蜂鸟为最新状态
		int fengniaoCallbackStatus = Integer.valueOf(String.valueOf(backDataMap.get("order_status")));

		switch (fengniaoCallbackStatus) {
			case 1:
				// 系统已接单
				log.info("系统已接单");
				deliverNote.setDeliverStatus(DeliveryStatus.WAIT_GET);
				deliveryNoteService.updateById(deliverNote);
				break;
			case 20:
				// 已分配骑手
				// 修改订单状态为配送中
				log.info("已分配骑手");
				deliverNote.setDeliverName(backDataMap.get("carrier_driver_name").toString());
				deliverNote.setDeliverPhone(backDataMap.get("carrier_driver_phone").toString());
				deliverNote.setReceiveTime(new Date());
				deliverNote.setDeliverStatus(DeliveryStatus.WAIT_GET);
				deliveryNoteService.updateById(deliverNote);
				break;
			case 80:
				// 骑手已到店
				log.info("骑手已到店");
				break;
			case 2:
				// 配送中
				deliverNote.setDeliverStatus(DeliveryStatus.TRANSFERING);
				deliveryNoteService.updateById(deliverNote);
				//修改订单状态为配送中
				baseOrderServiceFeign.transfering(deliverNote.getOrderId());
				break;
			case 3:
				// 已送达
				// 修改订单状态为已收货
				log.info("已送达");
				deliverNote.setDeliverStatus(DeliveryStatus.DONE);
				deliveryNoteService.updateById(deliverNote);
				//修改订单状态为已收货
				baseOrderServiceFeign.received(deliverNote.getOrderId());
				break;
			case 5:
				log.error("系统拒单");
				// 修改配送信息为失败
				deliverNote.setDeliverStatus(DeliveryStatus.FAILURE);
				deliverNote.setFailureCause(backDataMap.get("description").toString());
				deliverNote.setCancelTime(new Date());
				deliveryNoteService.updateById(deliverNote);
				break;
			default:
				log.error("蜂鸟回调返回其他状态");
				break;
		}
		return Tips.of(1,"配送回调处理成功");
	}

	/**
	 * @param hdOrderCode 订单code
	 * @Description: 查询蜂鸟订单详情
	 * @return: java.lang.String
	 * @Author: yj
	 * @Date: 2018/7/19
	 */
	public String detail(String hdOrderCode){
		ElemeQueryOrderRequest.ElemeQueryRequestData elemeQueryRequestData = new ElemeQueryOrderRequest.ElemeQueryRequestData();
		elemeQueryRequestData.setPartnerOrderCode(hdOrderCode);
		ResponseEntity<String> responseEntity = thirdPartyServiceFeign.getOrder(elemeQueryRequestData);
		if(Objects.nonNull(responseEntity)&&responseEntity.getStatusCode().is2xxSuccessful()){
			return responseEntity.getBody();
		}
		return "查询蜂鸟配送信息失败";
	}
}
