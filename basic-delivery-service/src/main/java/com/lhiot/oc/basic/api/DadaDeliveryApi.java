package com.lhiot.oc.basic.api;

import com.leon.microx.support.result.Tips;
import com.leon.microx.util.IOUtils;
import com.leon.microx.util.Jackson;
import com.leon.microx.util.SnowflakeId;
import com.leon.microx.util.StringUtils;
import com.leon.microx.util.auditing.MD5;
import com.lhiot.oc.basic.domain.DeliverNote;
import com.lhiot.oc.basic.feign.ThirdPartyServiceFeign;
import com.lhiot.oc.basic.service.DeliveryNoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RestController
@Api("达达配送api")
@RequestMapping("/dada-delivery")
public class DadaDeliveryApi {

    private final ThirdPartyServiceFeign thirdPartyServiceFeign;

    private final SnowflakeId snowflakeId;

    private final DeliveryNoteService deliveryNoteService;

    @Autowired
    public DadaDeliveryApi(DeliveryNoteService deliveryNoteService, ThirdPartyServiceFeign thirdPartyServiceFeign, SnowflakeId snowflakeId) {

        this.deliveryNoteService = deliveryNoteService;
        this.thirdPartyServiceFeign = thirdPartyServiceFeign;
        this.snowflakeId = snowflakeId;
    }
    /**
     * 达达配送回调
     *
     * @param request
     */
    @PostMapping("/callback")
    @ApiOperation(value = "达达配送回调", response = String.class)
    public ResponseEntity<?> callBack(HttpServletRequest request) {
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
        return thirdPartyServiceFeign.cancelOrderReasons();
    }

    @PostMapping("/cancel")
    @ApiOperation(value = "达达配送取消订单", response = String.class)
    public ResponseEntity<Tips> cancel(@RequestParam("orderId") Long orderId,
                                       @RequestParam("cancelReasonId") Integer cancelReasonId,
                                       @RequestParam("cancelReason") String cancelReason) {
        DeliverNote lastDeliverNote = deliveryNoteService.selectLastByOrderId(orderId);

        //向第三方取消配送
        ResponseEntity<String> responseEntity = thirdPartyServiceFeign.cancel(lastDeliverNote.getOrderCode(),cancelReasonId,cancelReason);

        if(Objects.isNull(responseEntity)||responseEntity.getStatusCodeValue()>=400){
            return ResponseEntity.ok(Tips.of(-1,"调用第三方取消配送失败"));
        }
        Map<String,Object> result = Jackson.map(responseEntity.getBody());
        // 取消成功
        if (Integer.valueOf(String.valueOf(result.get("code"))) == 0) {
            //String currentTime = DateFormatUtil.format1(new java.util.Date());
            //BaseOrderInfo order = baseOrderService.findOrderById(Long.valueOf(orderId));
            // 修改状态为待发货
//            order.set("order_status", "3");
//            order.update();
/*            failureCause
            cancelTime
            receiveTime
            deliverStatus*/

            /*DeliverNote deliverNote=new DeliverNote();
            deliverNote.setFailureCause(cancelReason);
            deliverNote.setCancelTime(new Date());
            deliverNote.setDeliverStatus(DeliverNote.DeliveryStatus.FAILURE);
            deliveryNoteService.updateById().updateNoteToFailure(request.getParameter("cancelReason"), currentTime, order.getId()+"");*/
            return ResponseEntity.ok(Tips.of(1,"取消成功"));
        } else {
            // 失败
            log.error("达达取消订单失败：{}" + result);
            return ResponseEntity.ok(Tips.of(-1,"达达取消订单失败"));
        }

    }

    @ApiOperation(value = "")
    @GetMapping("/send")
    public ResponseEntity sendDada(@RequestParam("hdOrderCode") String hdOrderCode) {
        //发送达达
        thirdPartyServiceFeign.addOrder();
        return ResponseEntity.ok(deliveryService.send(hdOrderCode));
    }


}
