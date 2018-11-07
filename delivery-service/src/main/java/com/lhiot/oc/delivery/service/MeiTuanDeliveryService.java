package com.lhiot.oc.delivery.service;

import com.leon.microx.util.Calculator;
import com.leon.microx.util.Jackson;
import com.leon.microx.web.result.Tips;
import com.lhiot.oc.delivery.domain.DeliverBaseOrder;
import com.lhiot.oc.delivery.domain.DeliverNote;
import com.lhiot.oc.delivery.domain.enums.CoordinateSystem;
import com.lhiot.oc.delivery.domain.enums.DeliverType;
import com.lhiot.oc.delivery.domain.enums.DeliveryStatus;
import com.lhiot.oc.delivery.feign.BasicDataService;
import com.lhiot.oc.delivery.meituan.MeiTuanDeliveryClient;
import com.lhiot.oc.delivery.meituan.model.*;
import com.lhiot.oc.delivery.meituan.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@Transactional
public class MeiTuanDeliveryService implements IDelivery {

    private final DeliveryNoteService deliveryNoteService;
    private final BasicDataService basicDataService;
    private final DeliverBaseOrderService deliverBaseOrderService;
    private final MeiTuanDeliveryClient meiTuanClient;


    public MeiTuanDeliveryService(DeliveryNoteService deliveryNoteService, BasicDataService basicDataService,
                                  DeliverBaseOrderService deliverBaseOrderService,
                                  MeiTuanDeliveryClient meiTuanClient) {
        this.basicDataService = basicDataService;
        this.meiTuanClient = meiTuanClient;
        this.deliveryNoteService = deliveryNoteService;
        this.deliverBaseOrderService = deliverBaseOrderService;
    }

    /**
     * 发送订单给美团(基于门店方式)
     * 门店对应为我们系统ERP中的门店
     *
     * @return Tips
     */
    public Tips send(CoordinateSystem coordinateSystem, DeliverBaseOrder deliverBaseOrder, BigDecimal distance) {
        log.info("发送美团的订单{}", deliverBaseOrder);
        /*ResponseEntity storeResponseEntity = basicDataService.findStoreByCode(deliverBaseOrder.getStoreCode(), deliverBaseOrder.getApplyType());
        if (storeResponseEntity.getStatusCode().isError() || Objects.isNull(storeResponseEntity.getBody())) {
            return Tips.warn("查询门店信息失败");
        }
        Store store = (Store) storeResponseEntity.getBody();*/
        //距离换算
        /*BigDecimal distance = Distance.getDistance(store.getStorePosition().getLat(), store.getStorePosition().getLng(), deliverBaseOrder.getLat(), deliverBaseOrder.getLng());
         */
        //记录配送信息
        DeliverNote deliverNote = new DeliverNote();
        deliverNote.setOrderId(deliverBaseOrder.getOrderId());
        deliverNote.setOrderCode(deliverBaseOrder.getOrderCode());//订单号
        deliverNote.setDeliverCode(deliverBaseOrder.getHdOrderCode());//配送单单号
        deliverNote.setDeliverType(DeliverType.MEITUAN);
        deliverNote.setStoreCode(deliverBaseOrder.getStoreCode());
        deliverNote.setRemark(deliverBaseOrder.getRemark());
        deliverNote.setFee(deliverBaseOrder.getDeliveryFee());//自己传递过来的配送费
        deliverNote.setDistance(distance.doubleValue());//自己计算的直线距离

        //创建配送单
        deliveryNoteService.createNewDeliverNote(deliverNote);

        CreateOrderByShopRequest createOrderByShopRequest = new CreateOrderByShopRequest();
        createOrderByShopRequest.setDeliveryId(deliverNote.getId());
        createOrderByShopRequest.setOrderId(deliverBaseOrder.getHdOrderCode());
        createOrderByShopRequest.setShopId(deliverBaseOrder.getStoreCode());//测试环境为"test_0001" 正式环境要换成真正的门店编码 deliverBaseOrder.getStoreCode()
        //配送服务代码，详情见合同
        //飞速达:4002
        //快速达:4011
        //及时达:4012
        //集中送:4013
        createOrderByShopRequest.setDeliveryServiceCode(4011);

        createOrderByShopRequest.setReceiverName(deliverBaseOrder.getReceiveUser());
        createOrderByShopRequest.setReceiverAddress(deliverBaseOrder.getAddress());
        createOrderByShopRequest.setReceiverPhone(deliverBaseOrder.getContactPhone());
        createOrderByShopRequest.setReceiverLng((int) (deliverBaseOrder.getLng() * 1000000));
        createOrderByShopRequest.setReceiverLat((int) (deliverBaseOrder.getLat() * 1000000));

        createOrderByShopRequest.setCoordinateType(coordinateSystem.getPositionSource() == 2 ? 1 : 0);//	坐标类型，0：火星坐标（高德，腾讯地图均采用火星坐标） 1：百度坐标 （默认值为0）

        createOrderByShopRequest.setGoodsValue(new BigDecimal(deliverBaseOrder.getTotalAmount() / 100.0).setScale(2));//分转元,精确两位小数
        //重量计算 所有商品的份数*重量*基础重量
        deliverBaseOrder.getDeliverOrderProductList().forEach(item ->
                createOrderByShopRequest.setGoodsWeight(createOrderByShopRequest.getGoodsWeight().add(BigDecimal.valueOf(Calculator.mul(Calculator.mul(item.getProductQty(), item.getStandardQty()), item.getBaseWeight()))))
        );
        createOrderByShopRequest.setGoodsWeight(createOrderByShopRequest.getGoodsWeight().setScale(2));

        //具体商品信息
        OpenApiGoods openApiGoods = new OpenApiGoods();
        List<OpenApiGood> openApiGoodList = new ArrayList<>(deliverBaseOrder.getDeliverOrderProductList().size());
        deliverBaseOrder.getDeliverOrderProductList().forEach(item -> {
            OpenApiGood openApiGood = new OpenApiGood();
            openApiGood.setGoodCount(item.getProductQty());
            openApiGood.setGoodName(item.getProductName());
            openApiGood.setGoodPrice(new BigDecimal(item.getDiscountPrice()));
            openApiGoodList.add(openApiGood);
        });
        openApiGoods.setGoods(openApiGoodList);
        createOrderByShopRequest.setGoodsDetail(openApiGoods);

        createOrderByShopRequest.setGoodsPickupInfo(deliverBaseOrder.getRemark());
        createOrderByShopRequest.setGoodsDeliveryInfo(deliverBaseOrder.getRemark());

        Map<String, Object> deliverTime = Jackson.map(deliverBaseOrder.getDeliverTime());

        createOrderByShopRequest.setExpectedPickupTime(DateUtil.fromDateStr(String.valueOf(deliverTime.get("startTime"))));
        createOrderByShopRequest.setExpectedDeliveryTime(DateUtil.fromDateStr(String.valueOf(deliverTime.get("endTime"))));

        //依据订单配送时间来决定是及时单还是预约单
        Date startTime = DateUtil.fromString(String.valueOf(deliverTime.get("startTime")));
        //当前的23:59:59
        Calendar todayCalendar = Calendar.getInstance();

        todayCalendar.set(todayCalendar.get(Calendar.YEAR), todayCalendar.get(Calendar.MONTH), todayCalendar.get(Calendar.DAY_OF_MONTH), 23, 59, 59);
        Date today = todayCalendar.getTime();
        if (startTime.after(today)) {
            createOrderByShopRequest.setOrderType(OrderType.PREBOOK.getCode());//订单类型，目前只支持预约单0: 及时单(尽快送达，限当日订单)1: 预约单
        } else {
            createOrderByShopRequest.setOrderType(OrderType.NORMAL.getCode());
        }
        int hdOrderCodeLength = deliverBaseOrder.getHdOrderCode().length();
        createOrderByShopRequest.setPoiSeq(deliverBaseOrder.getHdOrderCode().substring(hdOrderCodeLength - 4, hdOrderCodeLength));//骑手取货单号
        createOrderByShopRequest.setNote(deliverBaseOrder.getRemark());
        try {
            String response = meiTuanClient.deliver(createOrderByShopRequest);
            //发送美团返回结果
            CreateOrderResponse meiTuanOrderAddResult = Jackson.object(response, CreateOrderResponse.class);
            log.info("美团配送返回结果{}", meiTuanOrderAddResult);
            if (Objects.equals(meiTuanOrderAddResult.getCode(), "0")) {
                //更新配送信息
                DeliverNote updateDeliverNote = new DeliverNote();
                updateDeliverNote.setDeliverStatus(DeliveryStatus.UNRECEIVE);
                updateDeliverNote.setId(deliverNote.getId());
                updateDeliverNote.setExt(meiTuanOrderAddResult.getData().getMtPeisongId());//美团返回配送编码 用于模拟时传递的值
                deliveryNoteService.updateById(updateDeliverNote);
                //写入配送订单流程表
                deliverBaseOrderService.create(deliverBaseOrder);
                return Tips.info("创建美团配送成功");
            }
        } catch (IOException e) {
            log.error("美团发送配送失败,{}", e);
            return Tips.warn("美团发送配送失败");
        }
        return Tips.warn("创建美团配送失败");
    }


    /**
     * 取消配送订单原因列表
     *
     * @return string
     */
    public String cancelOrderReasons() {
        return "{\"result\":[\n" +
                "{\"code\":199,\"desc\":\"其他接入方原因\"},\n" +
                "{\"code\":299,\"desc\":\"其他美团配送原因\"}, \n" +
                "{\"code\":399,\"desc\":\"其他原因\"}, \n" +
                " ]}";
    }

    /**
     * @param hdOrderCode    订单hdCode
     * @param cancelReasonId 取消原因id
     * @param cancelReason   取消原因说明
     * @return Tips
     */
    public Tips cancel(String hdOrderCode, int cancelReasonId, String cancelReason) {
        DeliverNote searchDeliverNote = deliveryNoteService.selectByDeliverCode(hdOrderCode);
        if (Objects.isNull(searchDeliverNote)) {
            return Tips.of(-1, "未找到配送单信息");
        }

        CancelOrderRequest cancelOrderRequest = new CancelOrderRequest();
        cancelOrderRequest.setCancelOrderReasonId(CancelOrderReasonId.findByCode(cancelReasonId));
        cancelOrderRequest.setCancelReason(cancelReason);
        cancelOrderRequest.setDeliveryId(searchDeliverNote.getId());
        cancelOrderRequest.setMtPeisongId(searchDeliverNote.getExt());
        try {
            String cancel = meiTuanClient.cancel(cancelOrderRequest);
            log.error("取消美团配送,{}", cancel);
            CancelOrderResponse cancelOrderResponse=Jackson.object(cancel,CancelOrderResponse.class);
            if(Objects.equals(cancelOrderResponse.getCode(),"0")){
                //修改数据库中记录
                searchDeliverNote.setFailureCause(cancelReason);
                searchDeliverNote.setCancelTime(new Date());
                searchDeliverNote.setDeliverStatus(DeliveryStatus.FAILURE);
                deliveryNoteService.updateById(searchDeliverNote);
                return Tips.of(1, "取消美团配送成功");
            }else{
                return Tips.of(-1, "取消美团配送失败"+cancel);
            }
        } catch (IOException e) {
            log.error("取消美团配送失败,{e}", e);
            return Tips.of(-1, "取消美团配送失败");
        }

    }

    /**
     * 美团回调处理
     *
     * @param backMsg 回调JSON参数字符串
     * @return Tips
     */
    public Tips callback(String backMsg) {
        Map<String, Object> backMap = Jackson.map(backMsg);
        String dataBody = null;
        try {
            dataBody = URLDecoder.decode(backMap.get("data").toString(), StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            log.error("不支持的字符转换格式");
        }
        Map<String, Object> backDataMap = Jackson.map(dataBody);
        // 返回的订单编码
        String deliverCode = backDataMap.get("partner_order_code") + "";

        DeliverNote deliverNote = deliveryNoteService.selectByDeliverCode(deliverCode);

        if (Objects.isNull(deliverNote)) {
            log.error("未找到美团配送回调订单{}" + deliverCode);
            return Tips.of(-1, "未找到美团配送回调订单" + deliverCode);
        }
        // 修改美团为最新状态
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
                deliverNote.setDeliverName((String) backDataMap.get("carrier_driver_name"));
                deliverNote.setDeliverPhone((String) backDataMap.get("carrier_driver_phone"));
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
                deliverNote.setDeliverStatus(DeliveryStatus.DELIVERING);
                deliveryNoteService.updateById(deliverNote);
                //修改订单状态为配送中
                //orderService.delivering(deliverNote.getOrderId());
                break;
            case 3:
                // 已送达
                // 修改订单状态为已收货
                log.info("已送达");
                deliverNote.setDeliverStatus(DeliveryStatus.DONE);
                deliveryNoteService.updateById(deliverNote);
                //修改订单状态为已收货
                //orderService.received(deliverNote.getOrderId());
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
                log.error("美团回调返回其他状态");
                break;
        }
        return Tips.of(1, "配送回调处理成功");
    }

    /**
     * @param hdOrderCode 订单code
     * @return JSON String
     */
    public String detail(String hdOrderCode) {
        DeliverNote searchDeliverNote = deliveryNoteService.selectByDeliverCode(hdOrderCode);
        if (Objects.isNull(searchDeliverNote)) {
            return "未找到配送单信息";
        }
        QueryOrderRequest queryOrderRequest = new QueryOrderRequest();
        queryOrderRequest.setDeliveryId(searchDeliverNote.getId());
        queryOrderRequest.setMtPeisongId(searchDeliverNote.getExt());
        try {
            return meiTuanClient.one(queryOrderRequest);
        } catch (IOException e) {
            log.error("查询美团订单详情失败,{e}", e);
            return "查询美团订单详情失败";
        }
    }


}
