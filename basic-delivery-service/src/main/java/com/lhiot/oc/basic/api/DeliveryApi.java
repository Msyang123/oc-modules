package com.lhiot.oc.basic.api;

import com.leon.microx.support.result.Tips;
import com.leon.microx.util.IOUtils;
import com.leon.microx.util.StringUtils;
import com.leon.microx.util.auditing.MD5;
import com.lhiot.oc.basic.service.DeliveryNoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Slf4j
@RestController
@Api("配送api")
@RequestMapping("/delivery")
public class DeliveryApi {

    private final DeliveryUtil deliveryUtil;

    private final BaseOrderService baseOrderService;
    private final DeliveryService deliveryService;
    private final BaseServiceFeign baseServiceFeign;

    private final TwitterIdWorker orderNoGenerator = new TwitterIdWorker(0, 0);

    private final DeliveryNoteService deliveryNoteService;

    @Autowired
    public DeliveryApi(BaseOrderService baseOrderService, DeliveryService deliveryService,
                       DeliveryNoteService deliveryNoteService, DeliveryProperties deliveryProperties, BaseServiceFeign baseServiceFeign) {
        this.deliveryUtil = new DeliveryUtil(deliveryProperties);
        this.baseOrderService = baseOrderService;
        this.deliveryService = deliveryService;
        this.deliveryNoteService = deliveryNoteService;
        this.baseServiceFeign = baseServiceFeign;
    }
    /**
     * 达达配送回调
     *
     * @param request
     */
    @PostMapping("/callback/dada")
    @ApiOperation(value = "达达配送回调", response = String.class)
    public ResponseEntity<?> callBack(HttpServletRequest request) {
        Tips.info("")
        log.info("达达配送回调");
        try {
            InputStream result = request.getInputStream();
            // 转换成utf-8格式输出
            BufferedReader in = new BufferedReader(new InputStreamReader(result, "UTF-8"));
            List<String> lst = IOUtils.readLines(in);
            IOUtils.closeQuietly(result);
            String resultStr = StringUtils.join("", lst);
            log.info("backOrder-jsonData:" + resultStr);

            log.info("传入JSON字符串：" + resultStr);


        } catch (Exception e) {
            log.info("message = " + e.getMessage());
        }
        return ResponseEntity.ok("");
    }

    @PostMapping("/cancel/reasons")
    @ApiOperation(value = "达达配送取消原因列表", response = String.class)
    public ResponseEntity<?> cancelReasons() {
        // 读取配置信息
        DadaProps config = new DadaProps();
        config.setAppKey(deliveryUtil.getDeliveryProperties().getDada().getAppKey());
        config.setAppSecret(deliveryUtil.getDeliveryProperties().getDada().getAppSecret());
        config.setCharset(deliveryUtil.getDeliveryProperties().getDada().getCharset());
        config.setFormat(deliveryUtil.getDeliveryProperties().getDada().getFormat());
        config.setSourceId(deliveryUtil.getDeliveryProperties().getDada().getSourceId());
        config.setVersion(deliveryUtil.getDeliveryProperties().getDada().getVersion());
        config.setUrl(deliveryUtil.getDeliveryProperties().getDada().getUrl());
        config.setBackUrl(deliveryUtil.getDeliveryProperties().getDada().getBackUrl());
        // 达达处理类
        DadaDeliver dadaDeliver = new DadaDeliver(config);
        DadaService dadaService = new DadaService(dadaDeliver, JSON::toJSONString);

        return ResponseEntity.ok(dadaService.cancelOrderReasons());
    }

    @PostMapping("/cancel")
    @ApiOperation(value = "达达配送取消订单", response = String.class)
    public ResponseEntity<Tips> cancel(@RequestParam("cancelReasonId") Integer cancelReasonId,
                                       @RequestParam("orderCode") String orderCode,
                                       @RequestParam("cancelReason") String cancelReason
                                 ) {
        // 读取配置信息
        DadaProps config = new DadaProps();
        config.setAppKey(deliveryUtil.getDeliveryProperties().getDada().getAppKey());
        config.setAppSecret(deliveryUtil.getDeliveryProperties().getDada().getAppSecret());
        config.setCharset(deliveryUtil.getDeliveryProperties().getDada().getCharset());
        config.setFormat(deliveryUtil.getDeliveryProperties().getDada().getFormat());
        config.setSourceId(deliveryUtil.getDeliveryProperties().getDada().getSourceId());
        config.setVersion(deliveryUtil.getDeliveryProperties().getDada().getVersion());
        config.setUrl(deliveryUtil.getDeliveryProperties().getDada().getUrl());
        config.setBackUrl(deliveryUtil.getDeliveryProperties().getDada().getBackUrl());
        // 达达处理类
        DadaDeliver dadaDeliver = new DadaDeliver(config);
        DadaService dadaService = new DadaService(dadaDeliver, JSON::toJSONString);
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
            // 修改状态为待发货
//            order.set("order_status", "3");
//            order.update();
            // new TDeliverNote().updateNoteToFailure(request.getParameter("cancelReason"), currentTime, order.getId()+"");
            return ResponseEntity.ok(Tips.of(1,"取消成功"));
        } else {
            // 失败
            log.error("达达取消订单失败：" + result.toJSONString());
            return ResponseEntity.ok(Tips.of(-1,"达达取消订单失败：" + result.toJSONString()));
        }

    }

    /**
     * 蜂鸟配送回调
     */
    @PostMapping("/callback/fn")
    @ApiOperation(value = "蜂鸟回调", response = String.class)
    public ResponseEntity<?> fnCallBack(HttpServletRequest request){
        log.info("蜂鸟配送回调");
        try {
            InputStream result = request.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(result, "UTF-8"));
            List<String> list = IOUtils.readLines(in);
            IOUtils.closeQuietly(result);
            String resultStr = StringUtils.join("",list );
            log.info("callbackOrder-jsonData:" + resultStr);
            log.info("传入JSON字符串：" + resultStr);
            if(StringUtils.isNotBlank(resultStr)){
                resultStr = resultStr.substring(1,resultStr.length()-1);
            }
            //处理回调结果

            //通过redis获取蜂鸟配送token
            String fengniaoAccessToken=new RedisUtil(deliveryUtil.getDeliveryProperties().getAddr(), Integer.valueOf(deliveryUtil.getDeliveryProperties().getPort())).getToken();
            log.info("回调的accessToken："+fengniaoAccessToken);
            fengniaoAccessToken=fengniaoAccessToken.replaceAll("\\\\\"","\"").replaceAll("\"\\{","{").replaceAll("}\"","}");
            log.info("回调的accessToken replaceAll后："+fengniaoAccessToken);
            dealCallback(resultStr,fengniaoAccessToken);
        } catch (IOException e) {
            e.printStackTrace();
            log.info("message = " + e.getMessage());
        }
        return ResponseEntity.ok("");
    }

    /**
     * 蜂鸟回调业务处理
     * @param callbackStr
     * @throws IOException
     */
    private void dealCallback(String callbackStr,String accessToken) throws IOException {
        log.info(callbackStr);
        JSONObject callbackJson = JSONObject.parseObject(callbackStr);
        String appId = callbackJson.getString("app_id");
        String data = (String) callbackJson.get("data");
        String signature = callbackJson.getString("signature");
        String salt = callbackJson.getString("salt");


            // 判定签名是否正确 防止被篡改
            StringBuffer needSignatureStr = new StringBuffer();
            // 签名规则
            needSignatureStr
                    .append("app_id=")
                    .append(appId)
                    .append("&")
                    .append("access_token=")
                    .append(accessToken)
                    .append("&data=")
                    .append(data)
                    .append("&")
                    .append("salt=")
                    .append(salt);

        String calcSignature = MD5.md5(needSignatureStr.toString());
        log.info("---获取到的signature："+signature);
        log.info("---得到的calcSignature："+calcSignature);
        // 判断签名是否相等
        if (calcSignature.equals(signature)) {
            String dataBody = URLDecoder.decode(callbackJson.getString("data"), "utf-8");
            JSONObject jsonDataBody = JSONObject.parseObject(dataBody);
            // 返回的订单编码
            String orderCode = jsonDataBody.getString("partner_order_code");
            // 修改蜂鸟为最新状态
            log.info("蜂鸟回调数据： " + jsonDataBody);
            int fengniaoCallbackStatus = jsonDataBody.getIntValue("order_status");
            BaseOrderInfo order = baseOrderService.findOrderByCode(orderCode);
            DeliverNote deliverNote = deliveryNoteService.selectLastByOrderId(order.getId());
            switch (fengniaoCallbackStatus) {
                case 1:
                    // 系统已接单
                    log.info("系统已接单");
                    //订单状态：1-未付款，2-支付中，3-已付款，4-已收货，5-退货中，6-退货完成，7-取消中，8-海鼎退货中，9-海鼎退货失败，10-微信退款失败，11-订单成功，0-已失效, 12-配送中

                    // 0-未接单 1-待取货 2-配送中 3-配送完成 4-配送失败 5-未配送
                    if(OrderStatus.RETURNING.equals(order.getStatus())||Objects.equals(OrderStatus.ALREADY_RETURN,order.getStatus())){
                            //||"8".equals(order.getStr("order_status")
                        //deliverNote.updateStatusByOrderId("4", orderCode,1);
                        deliverNote.setDeliverStatus(DeliverNote.DeliveryStatus.FAILURE);
                        deliverNote.setFailureCause("商家取消");
                        deliveryNoteService.updateById(deliverNote);
                        //Db.update("update t_deliver_note set deliver_status=4,system_content='商家取消' where order_id=? and deliver_type=1",orderCode);
                    }else{
                        deliverNote.setDeliverStatus(DeliverNote.DeliveryStatus.WAIT_GET);
                        deliveryNoteService.updateById(deliverNote);
                    }
                    break;
                case 20:
                    // 已分配骑手
                    // 修改订单状态为配送中
                    log.info("已分配骑手");
                    deliverNote.setDeliverStatus(DeliverNote.DeliveryStatus.WAIT_GET);
                    deliveryNoteService.updateById(deliverNote);
                    // deliverNote.updateStatusByOrderId("1", orderCode,null);
//                    Db.update("update t_deliver_note set dada_deliver_name=?,dada_deliver_phone=? where order_id=? and deliver_type=1",
//                            jsonDataBody.getString("carrier_driver_name"),jsonDataBody.getString("carrier_driver_phone"),jsonDataBody.getString("partner_order_code"));
                    break;
                case 80:
                    // 骑手已到店
                    log.info("骑手已到店");
                    break;
                case 2:
                    // 配送中
                    deliverNote.setDeliverStatus(DeliverNote.DeliveryStatus.TRANSFERING);
                    deliveryNoteService.updateById(deliverNote);
//                    log.info("配送中");
//                    deliverNote.updateStatusByOrderId("2", orderCode,1);
                    break;
                case 3:
                    // 已送达
                    // 修改订单状态为已收货
                    log.info("已送达");
                    deliverNote.setDeliverStatus(DeliverNote.DeliveryStatus.DONE);
                    deliveryNoteService.updateById(deliverNote);
//                    deliverNote.updateStatusByOrderId("3", orderCode,1);
//                    order.setOrderStatus(OrderStatus.RECEIVED);//改变订单状态
//                    baseOrderService.updateOrder(order);
                    break;
                case 5:
                    log.error("系统拒单");
                    String currentTime = DateFormatUtil.format1(new Date());
                    // 修改配送信息为失败
                    deliverNote.setDeliverStatus(DeliverNote.DeliveryStatus.FAILURE);
                    deliverNote.setFailureCause(jsonDataBody.getString("description"));
                    deliveryNoteService.updateById(deliverNote);
//                    deliverNote.updateNoteToFailure(jsonDataBody.getString("description"), currentTime,
//                            orderCode);
                    // 蜂鸟系统拒单之后发达达
                    long time = 30*60*1000;//30分钟
                    Date now=new Date(new Date().getTime()+time);
                    String e=order.getDeliveryEndTime()+"";
                    String[] b=e.split(" ");
                    String date=b[0];//日期 2018-02-28
                    String[] d=b[1].split("-");
                    String time1=d[0];//时间 15:55
                    String deliverytime = date + " "+ time1+":00";
                    log.info("==============================看我");
                    //送货上门的单  当前时间加30钟大于等于开始配送时间
                    if("1".equals(order.getReceivingWay())&&(DateFormatUtil.format1(now)).compareTo(deliverytime)>=0){
                        log.info("蜂鸟拒单发送达达");
                        JSONObject addOrderResult =deliveryService.send(orderCode);
                        log.info("发达达返回状态"+addOrderResult);
                        if(addOrderResult!=null&&addOrderResult.getIntValue("code") == 0){//达达接单成功
                            log.info(addOrderResult.toJSONString());
                            //new DeliverNote().saveDeliverNote(order.getId()+"",order.getStoreCode(),2,"1",null,order.getDeliveryAmount());
//                            order.set("order_status", "12");//改变订单状态
//                            order.update();
                            order.setStatus(OrderStatus.RECEIVED);//改变订单状态
                            baseOrderService.updateOrderStatusById(order);
                        }
                    }else{
                        log.info("延时发送达达");
                        //new TDeliverNote().saveDeliverNote(order.getId()+"",order.getStoreCode(),2,"5","延时配送",order.getDeliveryFee());
                    }
                    break;
                default:
                    log.error("蜂鸟回调返回其他状态");
                    break;
            }
        } else {
            log.error("蜂鸟回调签名不正确" + callbackStr);
        }
    }

    @PostMapping("/queryDadaDeliverFee")
    @ApiOperation("达达配送依据距离和重量计算收费公式货全国配送")
    @ApiImplicitParam(paramType = "body", name = "orderParam", dataType = "CreateOrderParam", required = true, value = "创建订单传入参数")
    public ResponseEntity<?> queryDeliverFee(HttpServletRequest request, @RequestBody CreateOrderParam orderParam) {

        return ResponseEntity.ok("");
    }
    /**
     * 蜂鸟配送费计算
     * 通过蜂鸟的费用计算公式计算费用
     * 4.3 + 距离加价 + 重量加价 + 时段加价 = 蜂鸟配送费
     * 配送距离 加价规则（元/单）[0-1)km 0元 [1-2)km 1元 [2-3)km 2元 [3-4]km 4元
     * 重量 加价规则（元/单）[0-5] kg 0, (5-15] kg 每增加1KG加0.5元
     * 高峰时段 2元 11:00-13:00 22:00-02:00
     * 1、即时单以下单时间为区间判断的时间节点；
     * 2、预约单以要求送到时间为区间判断的时间节点；
     * 3、考虑到消费场景的可行性，暂不建议接02:00-9:00的订单
     * 4km以内，15kg以内才配送
     * 退货费用：非乙方及其配送团队原因产生的退货，甲方确认退货后要求乙方即时退回的，乙方收取甲方退货费为该订单基本配送费用的50%。
     * 基础费用 4.3d 测试费用0.01d
     * @throws UnsupportedEncodingException
     * @throws ParseException
     */
    @PostMapping("/queryFnDeliverFee")
    @ApiOperation("通过蜂鸟的费用计算公式计算费用")
    @ApiImplicitParam(paramType = "body", name = "orderParam", dataType = "CreateOrderParam", required = true, value = "创建订单传入参数")
    public ResponseEntity<?> queryFnDeliverFee(HttpServletRequest request, CreateOrderParam orderParam){
        int amount = 0;// 数量
        double weight = 0.0d;// 重量
        int orderPrice = 0;// 订单价格
        List<CreateOrderParam.OrderProductParam> productParams = orderParam.getOrderProducts();
        if (!CollectionUtils.isEmpty(productParams)) {
            for (CreateOrderParam.OrderProductParam orderProductParam:productParams) {
                int productNum = orderProductParam.getBuyCount();
                List<ProductsStandard> list= baseOrderService.findProductStardands(orderProductParam.getStandardId()+"");
                if(!CollectionUtils.isEmpty(list)) {
                    ProductsStandard pf = list.get(0);
                    //计算总重量
                    weight += pf.getBaseQty() * pf.getStandardQty()* productNum;
                    //计算总价格
                    orderPrice += pf.getSalePrice() * productNum;
                    amount++;
                }
            }
        }

/*        ElemeOpenConfig config=new ElemeOpenConfig();
        config.setAppKey(deliveryUtil.getDeliveryProperties().getFn().getAppKey());
        config.setAppSecret(deliveryUtil.getDeliveryProperties().getFn().getAppSecret());
        config.setBackUrl(deliveryUtil.getDeliveryProperties().getFn().getBackUrl());
        config.setCharset(deliveryUtil.getDeliveryProperties().getFn().getCharset());
        config.setUrl(deliveryUtil.getDeliveryProperties().getFn().getUrl());
        config.setVersion(deliveryUtil.getDeliveryProperties().getFn().getVersion());*/
        //距离换算
        boolean isConver=false; Double lat=0.0d;Double lng=0.0d;
        if(StringUtils.isNotBlank(request.getParameter("lat"))&& StringUtils.isNotBlank(request.getParameter("lng"))){
            lat=Double.valueOf(request.getParameter("lat"));
            lng=Double.valueOf(request.getParameter("lng"));
            isConver=false;
        }else{
            log.info("没有开启GPS 直接通过地址获取百度定位经纬度");
            JSONObject json = null;
            try {
                json = BaiduMapUtil.getLocation(orderParam.getAddress());
            } catch (Exception e) {
                e.printStackTrace();
            }
            log.info("百度查询地址调用："+json.toJSONString());
            if (json != null) {
                JSONObject addressResultJson = json.getJSONObject("result");
                JSONObject location = addressResultJson.getJSONObject("location");
                if (location != null) {
                    lat=Double.valueOf(location.get("lat").toString());
                    lng=Double.valueOf(location.get("lng").toString());
                    isConver=true;
                }
            }
        }

        String orderStore=orderParam.getStoreId()+"";
        StoreInfo storeInfo = baseOrderService.findStore(Long.valueOf(orderStore));
        double odistance=Double.valueOf(MapUtil.getDistance(storeInfo.getStoreCoordx(), storeInfo.getStoreCoordy(), ""+lat, ""+lng));
        log.info("门店到配送终点距离："+odistance);

        JSONObject result=new JSONObject();
        Double fee=4.3d;//基础费用
        //如果金额超过38元，免配送费
        if(orderPrice>=3800){
            fee=fee-4.3d;
        }

        if(weight<=5){
            log.info("重量在5kg之内！");
        }

        if(weight>5&&weight<=15){
            log.info("重量在5kg到15kg之内,每增加1KG加0.5元");
            fee = fee + Math.ceil(weight - 5) * 0.5;
        }

        if(odistance>4){
            log.info("超过配送范围！");
            result.put("code", -1);
            result.put("msg", "超过配送范围！");
            return ResponseEntity.ok(result);
        }

        if(weight>15){
            log.info("重量超过15kg,不接受配送");
            result.put("code", -1);
            result.put("msg", "重量超过15kg,不接受配送");
            return ResponseEntity.ok(result);
        }

        if(odistance<=1){
            log.info("配送在1km之内，不加钱");
        }

        if(odistance>1&&odistance<=2){
            log.info("配送在1km~2km之间，加1元");
            fee+=1;
        }

        if(odistance>2&&odistance<=3){
            log.info("配送在2km~3km之间，加2元");
            fee+=2;
        }

        if(odistance>3&&odistance<=4){
            log.info("配送在3km~4km之间，加4元");
            fee+=4;
        }

        String deliverTime = orderParam.getDeliveryTime();
        log.info("配送时间deliverTime"+deliverTime);
        String[] b=deliverTime.split(" ");
        String[] d=b[1].split("-");
        String time1=d[0];//时间 15:55
        Date deliverStart = DateFormatUtil.parse7(time1);//开始配送时间
        if(deliverStart.after(DateFormatUtil.parse7("11:00"))&&deliverStart.before(DateFormatUtil.parse7("13:00"))){
            log.info("高峰时段加2元");
            fee+=2;
        }
        log.info("------最终蜂鸟配送费："+fee);
        result.put("code", 0);
        result.put("fee", fee);
        return ResponseEntity.ok(result);
    }
    @ApiOperation(value = "")
    @GetMapping("/sendFn")
    public ResponseEntity sendFn(@RequestParam("hdOrderCode") String hdOrderCode) throws Exception {
        RedisUtil redisUtil = new RedisUtil(deliveryUtil.getDeliveryProperties().getAddr(), deliveryUtil.getDeliveryProperties().getPort());
        //正式环境配置，更新时注意
        String fengniaoAccessToken = redisUtil.getToken();//new RedisUtil(AppProps.get("addr"), AppProps.getInt("port")).getToken();
        log.info("accessToken的值：" + fengniaoAccessToken);
        fengniaoAccessToken = fengniaoAccessToken.replaceAll("\\\\\"", "\"").replaceAll("\"\\{", "{").replaceAll("}\"", "}");
        log.info("accessToken replaceAll之后的值：" + fengniaoAccessToken);

        return ResponseEntity.ok(deliveryService.sendFn(hdOrderCode, fengniaoAccessToken, null));
    }

    @ApiOperation(value = "")
    @GetMapping("/sendDada")
    public ResponseEntity sendDada(@RequestParam("hdOrderCode") String hdOrderCode) {
        return ResponseEntity.ok(deliveryService.send(hdOrderCode));
    }


}
