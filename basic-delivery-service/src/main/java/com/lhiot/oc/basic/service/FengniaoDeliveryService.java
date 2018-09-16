package com.lhiot.oc.basic.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leon.microx.support.result.Tips;
import com.leon.microx.util.Calculator;
import com.leon.microx.util.Jackson;
import com.lhiot.oc.basic.domain.DeliverBaseOrder;
import com.lhiot.oc.basic.domain.DeliverNote;
import com.lhiot.oc.basic.domain.enums.DeliverNeedConver;
import com.lhiot.oc.basic.domain.enums.DeliverType;
import com.lhiot.oc.basic.feign.BaseDataServiceFeign;
import com.lhiot.oc.basic.feign.BaseOrderServiceFeign;
import com.lhiot.oc.basic.feign.ThirdPartyServiceFeign;
import com.lhiot.oc.basic.feign.domain.ElemeCancelOrderRequest;
import com.lhiot.oc.basic.feign.domain.ElemeCreateOrderRequest;
import com.lhiot.oc.basic.feign.domain.FengniaoOrderAddResult;
import com.lhiot.oc.basic.feign.domain.Store;
import com.lhiot.oc.basic.util.Distance;
import com.lhiot.order.domain.BaseOrderInfo;
import com.lhiot.order.domain.DeliverNote;
import com.lhiot.order.domain.OrderProduct;
import com.lhiot.order.feign.domain.ProductsStandard;
import com.lhiot.order.feign.domain.StoreInfo;
import com.lhiot.order.util.BaiduMapUtil;
import com.lhiot.order.util.MapUtil;
import com.sgsl.components.dada.DadaApis;
import com.sgsl.components.dada.vo.OrderParam;
import com.sgsl.components.request.FengNiaoData;
import com.sgsl.components.request.Item;
import com.sgsl.components.request.Receiver;
import com.sgsl.components.request.Transport;
import com.sgsl.components.util.DateUtil;
import com.sgsl.components.util.MD5Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

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

		ResponseEntity<Store> storeResponseEntity = baseDataServiceFeign.findStoreByCode(deliverBaseOrder.getOrderCode(),deliverBaseOrder.getApplyType());
		if(Objects.isNull(storeResponseEntity)||storeResponseEntity.getStatusCode().isError()){
			return Tips.of(-1,"远程查询门店信息失败");
		}
		//获取到门店基础信息
		Store store = storeResponseEntity.getBody();
		//距离换算
		BigDecimal distance = Distance.getDistance(store.getStorePosition().getStoreCoordx(),store.getStorePosition().getStoreCoordy(),
				deliverBaseOrder.getCoordx(),deliverBaseOrder.getCoordy());

		//设置门店编码
		elemeCreateRequestData.setChainStoreCode(store.getStoreCode());
		//配送地址信息
		ElemeCreateOrderRequest.TransportInfo transportInfo = new ElemeCreateOrderRequest.TransportInfo();
		transportInfo.setAddress(store.getStoreAddress());
		transportInfo.setLatitude(store.getStorePosition().getStoreCoordx());
		transportInfo.setLongitude(store.getStorePosition().getStoreCoordy());
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
		receiverInfo.setLatitude(new BigDecimal(deliverBaseOrder.getCoordx()));
		receiverInfo.setLongitude(new BigDecimal(deliverBaseOrder.getCoordy()));
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

		//TODO 此处可以放入配置文件
		elemeCreateRequestData.setNotifyUrl("http://172.16.10.203:8211/thirdparty-service-v1-0/delivery/fengniao/callback");
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

		ElemeCancelOrderRequest.ElemeCancelOrderRequstData elemeCancelOrderRequstData=new ElemeCancelOrderRequest.ElemeCancelOrderRequstData();
		elemeCancelOrderRequstData.setPartnerOrderCode(hdOrderCode);
		elemeCancelOrderRequstData.setOrderCancelReasonCode(2);
		elemeCancelOrderRequstData.setOrderCancelCode(cancelReasonId);
		elemeCancelOrderRequstData.setOrderCancelDescription(cancelReason);
		elemeCancelOrderRequstData.setOrderCancelTime(System.currentTimeMillis());
		elemeCancelOrderRequstData.setOrderCancelNotifyUrl("http://172.16.10.203:8211/thirdparty-service-v1-0/delivery/fengniao/callback");


		return Tips.of(1,thirdPartyServiceFeign.cancel(elemeCancelOrderRequstData));
	}


	/**
	 * @param orderCode 订单code
	 * @Description: 查询蜂鸟订单详情
	 * @return: java.lang.String
	 * @Author: Limiaojun
	 * @Date: 2018/7/19
	 */
	public String detail(String orderCode,String accessToken) throws Exception {

		return fengniaoService.getOrder(orderCode,accessToken);
	}

	/**
	 * @Description: 构建蜂鸟接口参数
	 * @return: java.util.Map<java.lang.String   ,   java.lang.Object>
	 * @Author: Limiaojun
	 * @Date: 2018/7/19
	 */
/*	private String  buildFengNiaoApiParams(Map<String, Object> dataMap) throws IOException {
		FengniaoProps props = fengniaoDeliver.getProps();
		Map<String, Object> param = Maps.newHashMap();

		ObjectMapper om = new ObjectMapper();
		param.put("app_id", props.getAppKey());
		//salt
		Integer salt = Random.randomInteger(4);
		param.put("salt", salt);

		String data = om.writeValueAsString(dataMap);
		String escapeData = URLEncoder.encode(data, props.getCharset());
		param.put("data", escapeData);
		String fengniaoAccessToken = redisUtil.getToken();
		Map<String, Object> fengniaoAccessTokenMap = om.readValue(fengniaoAccessTokenStr, Map.class);
		Map<String, Object> accessTokenData = (Map<String, Object>) fengniaoAccessTokenMap.get("data");
		String accessToken = (String) accessTokenData.get("access_token");
		String app = "app_id=" + props.getAppKey() + "&access_token=" + accessToken + "&data=" + escapeData + "&salt=" + salt;
		String signature = MD5.md5(app);
		param.put("signature", signature);

		log.debug("发送蜂鸟参数：：：{}",param);
		return om.writeValueAsString(param);
	}*/

	public static void main(String[] args) {
		String x="app_id=4cdbc040657a4847b2667e31d9e2c3d9&access_token=9e7d0604-cb0d-4c7c-a09d-cf365ce6936c&data=%7B%22name%22%3A%22%E5%BC%A0%E4%B8%89%22%2C%22age%22%3A23%2C%22country%22%3A%22%E4%B8%AD%E5%9B%BD%22%7D&salt=1234";
		System.out.println(MD5Utils.getMD5Code(x));

		//{"app_id":"314cd925-2dd1-4c37-b66b-9af8e7fdbbd7","data":"%7B%22partner_remark%22%3Anull%2C%22partner_order_code%22%3A%221011598065615765504%22%2C%22notify_url%22%3A%22https%3A%2F%2Fweixin1.food-see.com%2Fweixin%2Fdada%2FfnCallBack%22%2C%22order_type%22%3A1%2C%22chain_store_code%22%3A%2207310106%22%2C%22transport_info%22%3A%7B%22transport_name%22%3A%22%E6%B0%B4%E6%9E%9C%E7%86%9F%E4%BA%86-%E5%B7%A6%E5%AE%B6%E5%A1%98%E5%BA%97%22%2C%22transport_address%22%3A%22%E9%95%BF%E6%B2%99%E5%B8%82%E9%9B%A8%E8%8A%B1%E5%8C%BA%E5%9F%8E%E5%8D%97%E4%B8%9C%E8%B7%AF220%E5%8F%B7%22%2C%22transport_longitude%22%3A112.998634%2C%22transport_latitude%22%3A28.179745%2C%22position_source%22%3A1%2C%22transport_tel%22%3A%2218975872045%22%2C%22transport_remark%22%3A%22%22%7D%2C%22order_add_time%22%3A1530018748000%2C%22order_total_amount%22%3A0.02%2C%22order_actual_amount%22%3A0.02%2C%22order_weight%22%3A0.5%2C%22order_remark%22%3Anull%2C%22is_invoiced%22%3A0%2C%22invoice%22%3Anull%2C%22order_payment_status%22%3A1%2C%22order_payment_method%22%3A1%2C%22is_agent_payment%22%3A0%2C%22require_payment_pay%22%3Anull%2C%22goods_count%22%3A1%2C%22require_receive_time%22%3A1530063000000%2C%22serial_number%22%3Anull%2C%22receiver_info%22%3A%7B%22receiver_name%22%3A%22ww%22%2C%22receiver_primary_phone%22%3A%2213636679347%22%2C%22receiver_second_phone%22%3Anull%2C%22receiver_address%22%3A%22%E6%B9%96%E5%8D%97%E7%9C%81%E9%95%BF%E6%B2%99%E5%B8%82%E6%A1%82%E8%8A%B1%E4%BA%8C%E6%9D%91%28%E5%9F%8E%E5%8D%97%E4%B8%9C%E8%B7%AF%E5%8C%97%29%E6%B0%B4%E6%9E%9C%E7%86%9F%E4%BA%86%28%E5%B7%A6%E5%AE%B6%E5%A1%98%E6%97%97%E8%88%B0%E5%BA%97%29%22%2C%22receiver_longitude%22%3A113.00914%2C%22receiver_latitude%22%3A28.179689%2C%22position_source%22%3A1%7D%2C%22items_json%22%3A%5B%7B%22item_id%22%3A%220108200199991%22%2C%22item_name%22%3A%22%E8%BF%9B%E5%8F%A3%E7%BA%A2%E6%8F%90%22%2C%22item_quantity%22%3A1%2C%22item_price%22%3A2.0%2C%22item_actual_price%22%3A2.0%2C%22item_size%22%3Anull%2C%22item_remark%22%3Anull%2C%22is_need_package%22%3A0%2C%22is_agent_purchase%22%3A0%2C%22agent_purchase_price%22%3Anull%7D%5D%7D","salt":2136,"signature":"162b444dcc01bf3d99753154a81dee6b"}
		String y="app_id=314cd925-2dd1-4c37-b66b-9af8e7fdbbd7&access_token=0901968b-6a6b-451c-afba-c6d61e8d9e02&data=%7B%22partner_remark%22%3Anull%2C%22partner_order_code%22%3A%221011598065615765504%22%2C%22notify_url%22%3A%22https%3A%2F%2Fweixin1.food-see.com%2Fweixin%2Fdada%2FfnCallBack%22%2C%22order_type%22%3A1%2C%22chain_store_code%22%3A%2207310106%22%2C%22transport_info%22%3A%7B%22transport_name%22%3A%22%E6%B0%B4%E6%9E%9C%E7%86%9F%E4%BA%86-%E5%B7%A6%E5%AE%B6%E5%A1%98%E5%BA%97%22%2C%22transport_address%22%3A%22%E9%95%BF%E6%B2%99%E5%B8%82%E9%9B%A8%E8%8A%B1%E5%8C%BA%E5%9F%8E%E5%8D%97%E4%B8%9C%E8%B7%AF220%E5%8F%B7%22%2C%22transport_longitude%22%3A112.998634%2C%22transport_latitude%22%3A28.179745%2C%22position_source%22%3A1%2C%22transport_tel%22%3A%2218975872045%22%2C%22transport_remark%22%3A%22%22%7D%2C%22order_add_time%22%3A1530018748000%2C%22order_total_amount%22%3A0.02%2C%22order_actual_amount%22%3A0.02%2C%22order_weight%22%3A0.5%2C%22order_remark%22%3Anull%2C%22is_invoiced%22%3A0%2C%22invoice%22%3Anull%2C%22order_payment_status%22%3A1%2C%22order_payment_method%22%3A1%2C%22is_agent_payment%22%3A0%2C%22require_payment_pay%22%3Anull%2C%22goods_count%22%3A1%2C%22require_receive_time%22%3A1530063000000%2C%22serial_number%22%3Anull%2C%22receiver_info%22%3A%7B%22receiver_name%22%3A%22ww%22%2C%22receiver_primary_phone%22%3A%2213636679347%22%2C%22receiver_second_phone%22%3Anull%2C%22receiver_address%22%3A%22%E6%B9%96%E5%8D%97%E7%9C%81%E9%95%BF%E6%B2%99%E5%B8%82%E6%A1%82%E8%8A%B1%E4%BA%8C%E6%9D%91%28%E5%9F%8E%E5%8D%97%E4%B8%9C%E8%B7%AF%E5%8C%97%29%E6%B0%B4%E6%9E%9C%E7%86%9F%E4%BA%86%28%E5%B7%A6%E5%AE%B6%E5%A1%98%E6%97%97%E8%88%B0%E5%BA%97%29%22%2C%22receiver_longitude%22%3A113.00914%2C%22receiver_latitude%22%3A28.179689%2C%22position_source%22%3A1%7D%2C%22items_json%22%3A%5B%7B%22item_id%22%3A%220108200199991%22%2C%22item_name%22%3A%22%E8%BF%9B%E5%8F%A3%E7%BA%A2%E6%8F%90%22%2C%22item_quantity%22%3A1%2C%22item_price%22%3A2.0%2C%22item_actual_price%22%3A2.0%2C%22item_size%22%3Anull%2C%22item_remark%22%3Anull%2C%22is_need_package%22%3A0%2C%22is_agent_purchase%22%3A0%2C%22agent_purchase_price%22%3Anull%7D%5D%7D&salt=2136";
		System.out.println(MD5Utils.getMD5Code(y));
	}
}
