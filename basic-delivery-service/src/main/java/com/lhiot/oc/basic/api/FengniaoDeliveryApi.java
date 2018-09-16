package com.lhiot.oc.basic.api;

import com.leon.microx.util.IOUtils;
import com.leon.microx.util.SnowflakeId;
import com.leon.microx.util.StringUtils;
import com.leon.microx.util.auditing.MD5;
import com.lhiot.oc.basic.domain.DeliverNote;
import com.lhiot.oc.basic.feign.ThirdPartyServiceFeign;
import com.lhiot.oc.basic.service.DeliveryNoteService;
import com.lhiot.oc.basic.service.FengniaoDeliveryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.URLDecoder;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Slf4j
@RestController
@Api("蜂鸟配送api")
@RequestMapping("/fengniao-delivery")
public class FengniaoDeliveryApi {

    private final FengniaoDeliveryService fengniaoDeliveryService;

    private final DeliveryNoteService deliveryNoteService;

    @Autowired
    public FengniaoDeliveryApi(DeliveryNoteService deliveryNoteService, FengniaoDeliveryService fengniaoDeliveryService) {

        this.deliveryNoteService = deliveryNoteService;
        this.fengniaoDeliveryService = fengniaoDeliveryService;
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

    @GetMapping("/cancel/reasons")
    @ApiOperation(value = "蜂鸟配送取消原因列表", response = String.class)
    public ResponseEntity<String> cancelReasons() {
        return ResponseEntity.ok(fengniaoDeliveryService.cancelOrderReasons());
    }

}
