package com.lhiot.oc.basic.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leon.microx.util.StringUtils;
import com.lhiot.order.domain.BaseOrderInfo;
import com.lhiot.order.domain.DeliverNote;
import com.lhiot.order.domain.OrderProduct;
import com.lhiot.order.feign.domain.ProductsStandard;
import com.lhiot.order.feign.domain.StoreInfo;
import com.lhiot.order.service.BaseOrderService;
import com.lhiot.order.util.BaiduMapUtil;
import com.lhiot.order.util.MapUtil;
import com.sgsl.components.FengniaoService;
import com.sgsl.components.config.ElemeOpenConfig;
import com.sgsl.components.dada.DadaApis;
import com.sgsl.components.dada.DadaDeliver;
import com.sgsl.components.dada.DadaProps;
import com.sgsl.components.dada.DadaService;
import com.sgsl.components.dada.vo.OrderParam;
import com.sgsl.components.request.FengNiaoData;
import com.sgsl.components.request.Item;
import com.sgsl.components.request.Receiver;
import com.sgsl.components.request.Transport;
import com.sgsl.components.util.DateUtil;
import com.sgsl.components.util.MD5Utils;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@Transactional
public class FengniaoDeliveryService implements IDelivery{
	private final RedissonClient redisson;
	private DadaProps dadaConfig=null;
	private ElemeOpenConfig fnconfig=null;
	private FengniaoService fengniaoService;
	private DadaService dadaService;
	private final BaseOrderService baseOrderService;
    private final DeliveryNoteService deliveryNoteService;

	public FengniaoDeliveryService(DeliveryProperties deliveryProperties, RedissonClient redisson, BaseOrderService baseOrderService, DeliveryNoteService deliveryNoteService){
		this.redisson = redisson;
		this.deliveryUtil = new DeliveryUtil(deliveryProperties);
        this.deliveryNoteService = deliveryNoteService;
        //dada读取配置信息
		dadaConfig=new DadaProps();
		dadaConfig.setAppKey(deliveryUtil.getDeliveryProperties().getDada().getAppKey());
		dadaConfig.setAppSecret(deliveryUtil.getDeliveryProperties().getDada().getAppSecret());
		dadaConfig.setCharset(deliveryUtil.getDeliveryProperties().getDada().getCharset());
		dadaConfig.setFormat(deliveryUtil.getDeliveryProperties().getDada().getFormat());
		dadaConfig.setSourceId(deliveryUtil.getDeliveryProperties().getDada().getSourceId());
		dadaConfig.setVersion(deliveryUtil.getDeliveryProperties().getDada().getVersion());
		dadaConfig.setUrl(deliveryUtil.getDeliveryProperties().getDada().getUrl());
		dadaConfig.setBackUrl(deliveryUtil.getDeliveryProperties().getDada().getBackUrl());

		dadaService=new DadaService(new DadaDeliver(dadaConfig), JSON::toJSONString);
		fnconfig = new ElemeOpenConfig();
		//蜂鸟读取配制信息
		fnconfig.setAppKey(deliveryUtil.getDeliveryProperties().getFn().getAppKey());
		fnconfig.setAppSecret(deliveryUtil.getDeliveryProperties().getFn().getAppSecret());
		fnconfig.setBackUrl(deliveryUtil.getDeliveryProperties().getFn().getBackUrl());
		fnconfig.setCharset(deliveryUtil.getDeliveryProperties().getFn().getCharset());
		fnconfig.setUrl(deliveryUtil.getDeliveryProperties().getFn().getUrl());
		//fnconfig.setVersion("/v2");
		fnconfig.setVersion(deliveryUtil.getDeliveryProperties().getFn().getVersion());

		fengniaoService=new FengniaoService(fnconfig);
		this.baseOrderService=baseOrderService;
	}
	/**
	 * 达达配送订单处理
	 * @return
	 */
	public JSONObject send(String hdOrderCode){
		BaseOrderInfo order = baseOrderService.findByHdCode(hdOrderCode,true,false,false,false,false);
		String deliveryAddress =order.getAddress();
    	log.info("订单达达配送："+order.getUserId()+"-"+hdOrderCode);

		//达达处理类


		OrderParam orderParam=new OrderParam();
		orderParam.setCallback(dadaService.getDadaDeliver().getProps().getBackUrl());
		double weight=0.0;
		for(OrderProduct orderProduct:order.getOrderProducts()){
			List<ProductsStandard> list= baseOrderService.findProductStardands(orderProduct.getStandardId()+"");
			if(!CollectionUtils.isEmpty(list)){
				ProductsStandard pf = list.get(0);
				weight+=pf.getBaseQty()* pf.getStandardQty()*orderProduct.getProductQty();
			}
			// 数量乘以单位重量

		}
		orderParam.setCargoNum(order.getOrderProducts().size());
		orderParam.setCargoPrice(order.getAmountPayable());//分
		orderParam.setCargoWeight(weight);
		orderParam.setCityCode("0731");
		orderParam.setInfo(order.getRemark());
		//long 经度  store_coordy
		//lat纬度   store_coordx
		StoreInfo storeInfo = baseOrderService.findStore(order.getStoreId());
		if(order.getStoreCoordx()!=null&&order.getStoreCoordy()!=null){
			orderParam.setLat(Double.valueOf(order.getStoreCoordx()));
			orderParam.setLng(Double.valueOf(order.getStoreCoordy()));
		}else{
			// 有地址，则根据地址获取经纬度 此处不能通过前端传递的实时定位经纬度处理 因为可能用户在路上下单
			JSONObject json = null;
				try {
					json = BaiduMapUtil.getLocation(deliveryAddress);
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (json != null) {
					JSONObject addressResultJson = json.getJSONObject("result");
					JSONObject location =addressResultJson.getJSONObject("location");
					if (location != null) {
						orderParam.setLat(Double.valueOf(location.get("lat").toString()));
						orderParam.setLng(Double.valueOf(location.get("lng").toString()));
					}
				}
		}
		log.info("达达配送经纬度lat"+storeInfo.getStoreCoordx()+"lng"+storeInfo.getStoreCoordy());
		log.info("lat:"+orderParam.getLat()+"--lng"+orderParam.getLng());
		double meDistance=Double.valueOf(MapUtil.getDistance(storeInfo.getStoreCoordx(), storeInfo.getStoreCoordy(), ""+orderParam.getLat(), ""+orderParam.getLng()));
		log.info("自己计算门店到收货地址距离："+meDistance);
		if(meDistance>5.0){
			JSONObject resultJson = new JSONObject();
			resultJson.put("code", -1);
			resultJson.put("msg", "自己计算距离"+meDistance+"超过配送5km范围");

			return resultJson;
		}
			orderParam.setOriginId(order.getHdOrderCode());
			orderParam.setOriginMark(order.getApplicationTypeEnum().name());//order.getStr("customer_note"));
			orderParam.setReceiverAddress(deliveryAddress);
			orderParam.setReceiverName(order.getReceiveUser());
			orderParam.setReceiverPhone(order.getContactPhone());
			orderParam.setReceiverTel("");

			orderParam.setShopNo(order.getStoreCode());

			//orderParam.setShopNo("11047059");

			JSONObject addOrderResult= null;
			try {
				//发送至达达配送 不需要再做转换坐标位置，因为已经使用的是腾讯坐标系，与达达的高德为同一个系
				addOrderResult= JSONObject.parseObject(dadaService.order(orderParam, DadaApis.API_ADD_ORDER, false));//dadaService.addOrder(orderParam)
				//String currentTime=DateFormatUtil.format1(new Date());
				//如果已经发过了，调用重复发单
				if(addOrderResult!=null&&2105==addOrderResult.getIntValue("code")){
					addOrderResult= JSONObject.parseObject(dadaService.order(orderParam, DadaApis.API_RE_ADD_ORDER, false));//.reAddOrder(orderParam);
					log.debug(addOrderResult.toJSONString());
					//更新订单信息在回调中处理
				}
				//记录配送信息
				deliveryNoteService.createNewDeliverNote(order,DeliverNote.DeliverType.DADA);
				log.info("===================>addOrderResult:"+addOrderResult.toJSONString());
				log.info(addOrderResult.toJSONString());
			} catch (IOException e) {
				e.printStackTrace();
				log.error("错了"+e.getMessage());
			}
			return addOrderResult;
	}

	/**
	 * 发送订单给蜂鸟
	 * @return
	 * @throws Exception
	 */
	public JSONObject sendFn(String hdOrderCode, String accessToken, String currentTime) throws Exception{
		log.info("发送蜂鸟的订单"+hdOrderCode);
    	FengNiaoData fengNiaoData = new FengNiaoData();
		BaseOrderInfo order = baseOrderService.findByHdCode(hdOrderCode,true,false,false,false,false);
        //开始设置data中的订单信息
        fengNiaoData.setPartnerRemark(order.getRemark());
        fengNiaoData.setPartnerOrderCode(order.getHdOrderCode());
        fengNiaoData.setNotifyUrl(fnconfig.getBackUrl());
        fengNiaoData.setOrderType(1);
        fengNiaoData.setChainStoreCode(order.getStoreCode());
        //设置transport
        Transport transport = new Transport();
		StoreInfo storeInfo = baseOrderService.findStore(order.getStoreId());
        transport.setName(storeInfo.getStoreName());
        transport.setAddress(storeInfo.getStoreAddress());
        transport.setLatitude(Double.valueOf(storeInfo.getStoreCoordx()));
        transport.setLongitude(Double.valueOf(storeInfo.getStoreCoordy()));
        transport.setPositionSource(1);
        transport.setTel(storeInfo.getStorePhone());
        transport.setRemark("");
        fengNiaoData.setTransport(transport);
        //订单明细
        List<Item> items = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(order.getCreateAt().getTime()));
        fengNiaoData.setOrderAddTime(calendar.getTimeInMillis());
        fengNiaoData.setOrderTotalAmount((order.getTotalAmount()/100.0));
        fengNiaoData.setOrderActualAmount((order.getAmountPayable()/100.0));

        fengNiaoData.setOrderRemark(order.getRemark());
        fengNiaoData.setInvoiced(0);
        fengNiaoData.setOrderPaymentStatus(1);
        fengNiaoData.setOrderPaymentMethod(1);
        fengNiaoData.setAgentPayment(0);

        //设置receiver
        Receiver receiver = new Receiver();
        receiver.setName(order.getReceiveUser());
        receiver.setPrimaryPhone(order.getContactPhone());
        receiver.setAddress(order.getAddress());

        //经度
        receiver.setLongitude(Double.valueOf(order.getCoordy()));
        //纬度
        receiver.setLatitude(Double.valueOf(order.getCoordx()));
        receiver.setPositionSource(1);
        fengNiaoData.setReceiver(receiver);
      //设置items
        double orderWeight=0.0;

		for(OrderProduct orderProduct:order.getOrderProducts()){
			Item item = new Item();
			List<ProductsStandard> list= baseOrderService.findProductStardands(orderProduct.getStandardId()+"");
			if(!CollectionUtils.isEmpty(list)) {
				ProductsStandard pf = list.get(0);
				orderWeight+=pf.getBaseQty()* pf.getStandardQty()*orderProduct.getProductQty();
				item.setId(pf.getProductCode());
				item.setName(pf.getProductName());
				item.setQuantity((new Double(orderProduct.getProductQty())).intValue());//订单商品数量
				item.setPrice((double)pf.getSalePrice());
				item.setActualPrice((double)order.getAmountPayable());
				item.setNeedPackage(0);
				item.setAgentPurchase(0);
				items.add(item);
			}
			// 数量乘以单位重量

		}
        //订单重量
        fengNiaoData.setOrderWeight(orderWeight);
        fengNiaoData.setItems(items);
        fengNiaoData.setGoodsCount(items.size());
        String deliveryTimeStart=null;
        if(StringUtils.isNotBlank(currentTime)){
        	deliveryTimeStart=currentTime;
        }else{
        	String a = order.getDeliveryTime()+"";
        	String[] b=a.split(" ");
        	String date=b[0];//日期 2018-02-28
        	String[] d=b[1].split("-");
        	String time=d[0];//时间 15:55:00
        	deliveryTimeStart = date + " "
        			+ time+":00";
        }
        log.info(deliveryTimeStart);
        // 配送到达时间加1小时
        long times = DateUtil.convertString2Date(deliveryTimeStart).getTime() + 60 * 60 * 1000;
        fengNiaoData.setRequireReceiveTime(times);

        //发送蜂鸟配送
        JSONObject obj= JSON.parseObject(fengniaoService.addOrder(fengNiaoData,accessToken));
        //记录配送信息
		deliveryNoteService.createNewDeliverNote(order,DeliverNote.DeliverType.FENGNIAO);
		log.info(obj.toJSONString());
		return obj;
	}


	/**
	 * @param orderCode      订单Code
	 * @param cancelReasonId 取消原因id
	 * @param cancelReason   取消原因说明
	 * @Description: 取消蜂鸟订单
	 * @return: java.lang.String
	 * @Author: Limiaojun
	 * @Date: 2018/7/19
	 */
	public String cancel(String orderCode, int cancelReasonId, String cancelReason) throws IOException {
		ObjectMapper om = new ObjectMapper();
		Map<String, Object> data = new HashMap<>();
		//商户订单号
		data.put("partner_order_code", orderCode);
		//订单取消原因代码(2:商家取消)
		data.put("order_cancel_reason_code", 2);
		//取消原因id
		data.put("order_cancel_code", cancelReasonId);
		//订单取消描述
		data.put("order_cancel_description", cancelReason);
		//订单取消时间（毫秒）
		data.put("order_cancel_time", new Date().getTime());


		return null;//fengniaoService.post("/order/cancel", buildFengNiaoApiParams(data));
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
