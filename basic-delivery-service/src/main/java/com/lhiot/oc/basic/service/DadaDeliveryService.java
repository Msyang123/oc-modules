package com.lhiot.oc.basic.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.leon.microx.common.wrapper.Multiple;
import com.leon.microx.common.wrapper.Tips;
import com.leon.microx.util.SnowflakeId;
import com.leon.microx.util.StringUtils;
import com.lhiot.order.domain.BaseOrderInfo;
import com.lhiot.order.domain.DeliverFee;
import com.lhiot.order.domain.DeliverNote;
import com.lhiot.order.domain.OrderProduct;
import com.lhiot.order.domain.enums.OrderStatus;
import com.lhiot.order.domain.inparam.CreateOrderParam;
import com.lhiot.order.feign.BaseServiceFeign;
import com.lhiot.order.feign.domain.Assortment;
import com.lhiot.order.feign.domain.ProductsStandard;
import com.lhiot.order.feign.domain.StoreInfo;
import com.lhiot.order.service.BaseOrderService;
import com.lhiot.order.util.BaiduMapUtil;
import com.lhiot.order.util.DeliverArea;
import com.lhiot.order.util.MapUtil;
import com.sgsl.components.dada.DadaApis;
import com.sgsl.components.dada.DadaDeliver;
import com.sgsl.components.dada.DadaProps;
import com.sgsl.components.dada.DadaService;
import com.sgsl.components.dada.vo.OrderParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class DadaDeliveryService implements IDelivery {

    private DadaService dadaService;
    private final BaseOrderService baseOrderService;
    private final DeliveryNoteService deliveryNoteService;
    private final BaseServiceFeign baseServiceFeign;
    private final SnowflakeId snowflakeId;

    public DadaDeliveryService(DeliveryProperties deliveryProperties, BaseOrderService baseOrderService, DeliveryNoteService deliveryNoteService, BaseServiceFeign baseServiceFeign, SnowflakeId snowflakeId) {
        this.deliveryNoteService = deliveryNoteService;
        this.baseServiceFeign = baseServiceFeign;
        this.snowflakeId = snowflakeId;
        //dada读取配置信息
        DadaProps dadaConfig = new DadaProps();
        dadaConfig.setAppKey(deliveryProperties.getDada().getAppKey());
        dadaConfig.setAppSecret(deliveryProperties.getDada().getAppSecret());
        dadaConfig.setCharset(deliveryProperties.getDada().getCharset());
        dadaConfig.setFormat(deliveryProperties.getDada().getFormat());
        dadaConfig.setSourceId(deliveryProperties.getDada().getSourceId());
        dadaConfig.setVersion(deliveryProperties.getDada().getVersion());
        dadaConfig.setUrl(deliveryProperties.getDada().getUrl());
        dadaConfig.setBackUrl(deliveryProperties.getDada().getBackUrl());

        dadaService = new DadaService(new DadaDeliver(dadaConfig), JSON::toJSONString);
        this.baseOrderService = baseOrderService;
    }

    /**
     * 达达配送订单处理
     *
     * @return
     */
    public String send(String hdOrderCode) {
        BaseOrderInfo order = baseOrderService.findByHdCode(hdOrderCode, true, false, false, false, false);
        String deliveryAddress = order.getAddress();
        log.info("订单达达配送：" + order.getUserId() + "-" + hdOrderCode);

        //达达处理类


        OrderParam orderParam = new OrderParam();
        orderParam.setCallback(dadaService.getDadaDeliver().getProps().getBackUrl());
        double weight = 0.0;
        for (OrderProduct orderProduct : order.getOrderProducts()) {
            List<ProductsStandard> list = baseOrderService.findProductStardands(orderProduct.getStandardId() + "");
            if (!CollectionUtils.isEmpty(list)) {
                ProductsStandard pf = list.get(0);
                weight += pf.getBaseQty() * pf.getStandardQty() * orderProduct.getProductQty();
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
        if (order.getStoreCoordx() != null && order.getStoreCoordy() != null) {
            orderParam.setLat(Double.valueOf(order.getStoreCoordx()));
            orderParam.setLng(Double.valueOf(order.getStoreCoordy()));
        } else {
            // 有地址，则根据地址获取经纬度 此处不能通过前端传递的实时定位经纬度处理 因为可能用户在路上下单
            JSONObject json = null;
            try {
                json = BaiduMapUtil.getLocation(deliveryAddress);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (json != null) {
                JSONObject addressResultJson = json.getJSONObject("result");
                JSONObject location = addressResultJson.getJSONObject("location");
                if (location != null) {
                    orderParam.setLat(Double.valueOf(location.get("lat").toString()));
                    orderParam.setLng(Double.valueOf(location.get("lng").toString()));
                }
            }
        }
        log.info("达达配送经纬度lat" + storeInfo.getStoreCoordx() + "lng" + storeInfo.getStoreCoordy());
        log.info("lat:" + orderParam.getLat() + "--lng" + orderParam.getLng());
        double meDistance = Double.valueOf(MapUtil.getDistance(storeInfo.getStoreCoordx(), storeInfo.getStoreCoordy(), "" + orderParam.getLat(), "" + orderParam.getLng()));
        log.info("自己计算门店到收货地址距离：" + meDistance);
        if (meDistance > 5.0) {
            JSONObject resultJson = new JSONObject();
            resultJson.put("code", -1);
            resultJson.put("msg", "自己计算距离" + meDistance + "超过配送5km范围");

            return resultJson.toJSONString();
        }
        orderParam.setOriginId(order.getHdOrderCode());
        orderParam.setOriginMark(order.getApplicationTypeEnum().name());//order.getStr("customer_note"));
        orderParam.setReceiverAddress(deliveryAddress);
        orderParam.setReceiverName(order.getReceiveUser());
        orderParam.setReceiverPhone(order.getContactPhone());
        orderParam.setReceiverTel("");

        orderParam.setShopNo(order.getStoreCode());

        //orderParam.setShopNo("11047059");

        JSONObject addOrderResult = null;
        try {
            //发送至达达配送 不需要再做转换坐标位置，因为已经使用的是腾讯坐标系，与达达的高德为同一个系
            addOrderResult = JSONObject.parseObject(dadaService.order(orderParam, DadaApis.API_ADD_ORDER, false));//dadaService.addOrder(orderParam)
            //String currentTime=DateFormatUtil.format1(new Date());
            //如果已经发过了，调用重复发单
            if (addOrderResult != null && 2105 == addOrderResult.getIntValue("code")) {
                addOrderResult = JSONObject.parseObject(dadaService.order(orderParam, DadaApis.API_RE_ADD_ORDER, false));//.reAddOrder(orderParam);
                log.debug(addOrderResult.toJSONString());
                //更新订单信息在回调中处理
            }
            //记录配送信息
            deliveryNoteService.createNewDeliverNote(order, DeliverNote.DeliverType.DADA);
            log.info("===================>addOrderResult:" + addOrderResult.toJSONString());
            log.info(addOrderResult.toJSONString());
        } catch (IOException e) {
            e.printStackTrace();
            log.error("错了" + e.getMessage());
        }
        return addOrderResult.toJSONString();
    }


    /**
     * @param orderCode      订单Code
     * @param cancelReasonId 取消原因id
     * @param cancelReason   取消原因说明
     * @Description: 取消达达平台配送订单
     * @return: Tips
     * @Author: yj
     * @Date: 2018/7/19
     */
    public Tips cancel(String orderCode, int cancelReasonId, String cancelReason) {
        JSONObject result = null;
        try {
            result = JSONObject.parseObject(dadaService.formalCancel(orderCode, cancelReasonId, cancelReason));
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        // 取消成功
        if (result.getIntValue("code") == 0) {
            //String currentTime = DateFormatUtil.format1(new java.util.Date());
            //BaseOrderInfo order = baseOrderService.findOrderById(Long.valueOf(orderId));
            //TODO 修改状态为待发货
//            order.set("order_status", "3");
//            order.update();
            // new TDeliverNote().updateNoteToFailure(request.getParameter("cancelReason"), currentTime, order.getId()+"");
            return Tips.of(1, "取消成功");
        } else {
            // 失败
            log.error("达达取消订单失败：" + result.toJSONString());
            return Tips.of(-1, "达达取消订单失败：" + result.toJSONString());
        }
    }

    public Tips callBack(String backMsg) {
        if (StringUtils.isNotBlank(backMsg)) {
            backMsg = backMsg.substring(1, backMsg.length() - 1);
        }
        JSONObject getJsonVal = JSONObject.parseObject(backMsg);
        log.info("getJsonVal = " + getJsonVal.toString());

        Map<String, Object> param = new HashMap<String, Object>();
        param.put("client_id", getJsonVal.getString("client_id"));
        param.put("order_id", getJsonVal.getString("order_id"));
        param.put("update_time", getJsonVal.getString("update_time"));
        param.put("signature", getJsonVal.getString("signature"));

        // 读取配置信息
/*		DadaProps config = new DadaProps();
		config.setAppKey(deliveryUtil.getDeliveryProperties().getDada().getAppKey());
		config.setAppSecret(deliveryUtil.getDeliveryProperties().getDada().getAppSecret());
		config.setCharset(deliveryUtil.getDeliveryProperties().getDada().getCharset());
		config.setFormat(deliveryUtil.getDeliveryProperties().getDada().getFormat());
		config.setSourceId(deliveryUtil.getDeliveryProperties().getDada().getSourceId());
		config.setVersion(deliveryUtil.getDeliveryProperties().getDada().getVersion());
		config.setUrl(deliveryUtil.getDeliveryProperties().getDada().getUrl());
		config.setBackUrl(deliveryUtil.getDeliveryProperties().getDada().getBackUrl());
		// 达达处理类
		DadaDeliver dadaDeliver = new DadaDeliver(config);*/
        // 判定签名是否正确 防止被篡改
        if (dadaService.getDadaDeliver().backSginature(param)) {
            String orderCode = getJsonVal.getString("order_id");
            BaseOrderInfo order = baseOrderService.findOrderByCode(orderCode, false, false, false, false, false);
            DeliverNote deliverNote = deliveryNoteService.selectLastByOrderId(order.getId());
            if (order == null) {
                log.error("未找到达达配送回调订单:" + orderCode);
            } else {
                // 修改达达为最新状态
                // 达达配送状态 待接单＝1 待取货＝2 配送中＝3 已完成＝4 已取消＝5 已过期＝7 指派单=8
                // 系统故障订单发布失败=1000 可参考文末的状态说明
                switch (getJsonVal.getIntValue("order_status")) {
                    // 待接单
                    case 1:
                        deliverNote.setDeliverStatus(DeliverNote.DeliveryStatus.UNRECEIVE);
                        deliveryNoteService.updateById(deliverNote);
                        break;
                    // 待取货
                    case 2:
                        deliverNote.setDeliverName(getJsonVal.getString("dm_name"));
                        deliverNote.setDeliverPhone(getJsonVal.getString("dm_mobile"));
                        deliverNote.setDeliverStatus(DeliverNote.DeliveryStatus.WAIT_GET);
                        deliveryNoteService.updateById(deliverNote);
                        break;
                    // 配送中
                    case 3:
                        deliverNote.setDeliverStatus(DeliverNote.DeliveryStatus.TRANSFERING);
                        deliveryNoteService.updateById(deliverNote);
                        break;
                    // 配送完成
                    case 4:
                        deliverNote.setDeliverStatus(DeliverNote.DeliveryStatus.DONE);
                        deliveryNoteService.updateById(deliverNote);
                        order.setRecieveTime(new Timestamp(new Date().getTime()));
                        //修改订单状态为已收货
                        order.setStatus(OrderStatus.RECEIVED);
                        baseOrderService.update(order);
                        break;
                    // 已取消
                    case 5:
                        // 已过期
                    case 7:

                        // 系统故障订单发布失败
                    case 1000:
                        //String currentTime = DateFormatUtil.format1(new java.util.Date());
                        // 修改配送信息为失败
                        deliverNote.setDeliverStatus(DeliverNote.DeliveryStatus.FAILURE);
                        deliverNote.setFailureCause(getJsonVal.getString("cancel_reason"));
                        deliverNote.setCancelTime(new Date());
                        deliveryNoteService.updateById(deliverNote);
                        break;
                    default:
                        break;
                }
            }
        } else {
            log.error("达达配送回调签名验证错误");
        }
        return null;
    }

    /**
     * 查询配送费
     *
     * @param deliveryAddress
     * @param orderParam
     * @return
     */
    public Tips<DeliverFee> queryDeliverFee(String deliveryAddress, CreateOrderParam orderParam) {
        int count = 0;// 数量
        double weight = 0.0d;// 重量
        int orderPrice = 0;// 订单价格
        if (orderParam.getAssortments() == null || CollectionUtils.isEmpty(orderParam.getAssortments())) {
            //计算非套餐商品
            List<String> standardIdList = orderParam.getOrderProducts().parallelStream()
                    .map(CreateOrderParam.OrderProductParam::getStandardId)
                    .map(String::valueOf).collect(Collectors.toList());
            String standardIds = StringUtils.arrayToDelimitedString(StringUtils.toStringArray(standardIdList), ",");
            if (StringUtils.isNotBlank(standardIds)) {
                List<ProductsStandard> baseProductsStandardList = baseOrderService.findProductStardands(standardIds);
                for (CreateOrderParam.OrderProductParam orderProductParam : orderParam.getOrderProducts()) {
                    count += orderProductParam.getBuyCount();
                    for (ProductsStandard productsStandard : baseProductsStandardList) {
                        if (Objects.equals(orderProductParam.getStandardId(), productsStandard.getId())) {
                            //计算总价格
                            orderPrice += productsStandard.getSalePrice() * orderProductParam.getBuyCount();
                            //计算总重量
                            weight += productsStandard.getBaseQty() * productsStandard.getStandardQty() * orderProductParam.getBuyCount();
                            break;
                        }
                    }
                }
            }
        } else {
            //计算套餐配送费用
            List<CreateOrderParam.OrderAssortmentParam> assortments = orderParam.getAssortments();
            List<String> assortmentIdList = assortments.stream().map(CreateOrderParam.OrderAssortmentParam::getAssortmentId).map(String::valueOf).collect(Collectors.toList());
            String assortmentIds = StringUtils.arrayToDelimitedString(StringUtils.toStringArray(assortmentIdList), ",");
            ResponseEntity<Multiple<Assortment>> baseAssortmentsList = baseServiceFeign.findAssortments(assortmentIds, "yes");

            //计算重量和订单金额
            for (CreateOrderParam.OrderAssortmentParam assortment : assortments) {
                int buyCount = assortment.getBuyCount();
                int price = assortment.getPrice();
                orderPrice += buyCount * price;
                count += buyCount;

                for (Assortment baseAssortment : baseAssortmentsList.getBody().getArray()) {
                    //查找购买套餐中对应的基础套餐信息
                    if (Objects.equals(baseAssortment.getId(), assortment.getAssortmentId())) {
                        //循环套餐商品
                        for (ProductsStandard productsStandard : baseAssortment.getAssortmentProducts()) {
                            weight = productsStandard.getStandardQty() * productsStandard.getRelationCount() * buyCount;
                        }
                        break;
                    }
                }
            }
        }


        // 发送预下单到达达
        OrderParam orderParamVo = new OrderParam();
        orderParamVo.setCallback(dadaService.getDadaDeliver().getProps().getBackUrl());
        orderParamVo.setCargoNum(count);
        orderParamVo.setCargoPrice(orderPrice);
        orderParamVo.setCargoWeight(weight);
        orderParamVo.setCityCode("0731");
        orderParamVo.setInfo("");
        boolean isConver = false;
        if (StringUtils.isNotBlank(orderParam.getStoreCoordx() + "") && StringUtils.isNotBlank(orderParam.getStoreCoordy() + "")) {
            orderParamVo.setLat(orderParam.getStoreCoordx());
            orderParamVo.setLng(orderParam.getStoreCoordy());
            isConver = false;
        } else {
            log.info("没有开启GPS 直接通过地址获取百度定位经纬度");
            JSONObject json = null;
            try {
                json = BaiduMapUtil.getLocation(deliveryAddress);
            } catch (Exception e) {
                e.printStackTrace();
            }
            log.info("百度查询地址调用：" + json.toJSONString());
            if (json != null) {
                JSONObject addressResultJson = json.getJSONObject("result");
                JSONObject location = addressResultJson.getJSONObject("location");
                if (location != null) {
                    orderParamVo.setLat(Double.valueOf(location.get("lat").toString()));
                    orderParamVo.setLng(Double.valueOf(location.get("lng").toString()));
                    isConver = true;
                }
            }
        }
        // 预调用处理，不传递正式订单编号
        orderParamVo.setOriginId(snowflakeId.stringId() + "0");
        orderParamVo.setOriginMark(orderParam.getApplicationTypeEnum().toString());
        orderParamVo.setReceiverAddress(orderParam.getAddress());
        orderParamVo.setReceiverName(orderParam.getReceiveUser());
        orderParamVo.setReceiverPhone(orderParam.getContactPhone());
        //远程查询门店信息
        StoreInfo storeInfo = baseOrderService.findStore(orderParam.getStoreId());
        orderParamVo.setShopNo(storeInfo.getStoreCode());


        log.info("门店的距离和我的距离" + storeInfo.getStoreCoordx() + "==" + storeInfo.getStoreCoordy() + "==" + orderParamVo.getLat() + "==" + orderParamVo.getLng());
        double meDistance = Double.valueOf(MapUtil.getDistance(storeInfo.getStoreCoordx(), storeInfo.getStoreCoordy(), "" + orderParamVo.getLat(), "" + orderParamVo.getLng()));

        log.info("-------------距离：" + meDistance);
        log.info("商品总重量：" + weight + "kg;----自己计算门店到收货地址距离：" + meDistance + "----" + deliveryAddress + "----" + orderParam.getStoreId() +
                "---" + orderParamVo.getLat() + "---" + orderParamVo.getLng());
        if (meDistance > 5.0) {
            JSONObject resultJson = new JSONObject();
            resultJson.put("code", -1);
            resultJson.put("msg", "超过配送5km范围");
            return Tips.of(-1, "超过配送5km范围");
        }
        JSONObject result = null;
        try {
            result = JSONObject.parseObject(dadaService.queryDeliverFee(orderParamVo, isConver));
            log.info("达达配送费用查询调用：" + result.toJSONString());
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        if (result.getIntValue("code") == 0) {
            int needPayFee;
            JSONObject resultBody = result.getJSONObject("result");
            // 配送距离
            double distance = resultBody.getDouble("distance");
            int resultFee = (int) (resultBody.getDouble("fee") * 100);
            // 配送重量
            JSONObject resultJson = new JSONObject();
            resultJson.put("code", -1);
            if (weight < 5.0) {
                DeliverArea area = DeliverArea.match(distance);
                if (area == null) {
                    return Tips.of(-1, "超过达达配送5km范围");
                }
                needPayFee = area.reduceFee(resultFee, new int[]{0, 0, 3, 5, 7});
            } else if (weight >= 5.0 && weight <= 25) {
                DeliverArea area = DeliverArea.match(distance);
                if (area == null) {
                    return Tips.of(-1, "超过配送5km范围");
                }
                needPayFee = area.reduceFee(resultFee, new int[]{5, 6, 3, 5, 7});
            } else {
                return Tips.of(-1, "超过配送25kg重量范围");
            }

            //TODO result.getJSONObject("result").put("fee", 0.01);
            DeliverFee deliverFee = new DeliverFee((needPayFee < 0 ? 0 : needPayFee), orderParamVo.getLat(), orderParamVo.getLng());

            Tips<DeliverFee> tips = new Tips();
            tips.data(deliverFee);
            tips.setCode("1");
            return tips;
        } else {
            return Tips.of(-1, "请重试");
        }
    }

    /**
     * @param hdOrderCode 订单信息
     * @Description: 查询达达订单详情
     * @return: java.lang.String
     * @Author: Limiaojun
     * @Date: 2018/7/19
     */
    public String detail(String hdOrderCode) {
        try {
            return dadaService.orderStatus(hdOrderCode);
        } catch (IOException e) {
            log.error("查询平台配送单信息错误,{}", e);
            return null;
        }

    }
}
