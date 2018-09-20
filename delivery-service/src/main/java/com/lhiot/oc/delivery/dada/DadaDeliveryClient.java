package com.lhiot.oc.delivery.dada;

import com.leon.microx.util.Maps;
import com.leon.microx.util.Pair;
import com.lhiot.oc.delivery.dada.vo.OrderParam;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Leon (234239150@qq.com) created in 12:21 18.9.18
 */
public class DadaDeliveryClient {

    private DadaDeliverHelper dadaDeliverHelper;

    private Function<Map<String, Object>, String> jsonConverter;

    public DadaDeliveryClient(DadaDeliverHelper dadaDeliverHelperHelper, Function<Map<String, Object>, String> jsonConverter) {
        this.dadaDeliverHelper = dadaDeliverHelperHelper;
        this.jsonConverter = jsonConverter;
    }

    /**
     * 取消订单<br/>
     * 在订单待接单或待取货情况下，调用此接口可取消订单。注意：接单后1－15分钟内取消订单，运费退回。同时扣除2元作为给配送员的违约金
     *
     * @param orderId        订单ID
     * @param cancelReasonId 取消原因ID
     * @param cancelReason   取消原因
     * @return JSON String
     * @throws IOException
     */
    public String cancel(String orderId, int cancelReasonId, String cancelReason) throws IOException {
        Map<String, Object> data = this.dadaDeliverHelper.sign(this.jsonConverter.apply(
                Maps.of("order_id", orderId, "cancel_reason_id", cancelReasonId, "cancel_reason", cancelReason)
        ));
        return dadaDeliverHelper.post(this.jsonConverter.apply(data), DadaServerApi.API_ORDER_FORMAL_CANCEL);
    }

    /**
     * 退单原因列表
     *
     * @return JSON String
     * @throws IOException
     */
    public String cancelReasons() throws IOException {
        Map<String, Object> data = this.dadaDeliverHelper.sign("");
        return dadaDeliverHelper.post(this.jsonConverter.apply(data), DadaServerApi.API_ORDER_CANCEL_REASONS);
    }

    /**
     * 订单详情 [状态] 查询
     *
     * @param orderId 订单ID
     * @return JSON String
     * @throws IOException
     */
    public String one(String orderId) throws IOException {
        Map<String, Object> data = this.dadaDeliverHelper.sign(this.jsonConverter.apply(Maps.of("order_id", orderId)));
        return dadaDeliverHelper.post(this.jsonConverter.apply(data), DadaServerApi.API_ORDER_STATUS_QUERY);
    }

    /*
     * 下单
     *
     * @param orderParam 下单参数
     * @param isGcj02    是否需要转成高德系标准 百度坐标系需要，腾讯坐标系不需要
     * @return JSON String
     * @throws IOException
     */
    private String send(OrderParam orderParam, String api, boolean isGcj02) throws IOException {
        Pair<Double, Double> converted = this.dadaDeliverHelper.convertCoordinates(orderParam.getLng(), orderParam.getLat(), isGcj02);
        Map<String, Object> data = this.dadaDeliverHelper.sign(this.jsonConverter.apply(
                Maps.<String, Object>builder()
                        .put("shop_no", orderParam.getShopNo())
                        .put("origin_id", orderParam.getOriginId())
                        .put("city_code", orderParam.getCityCode())
                        .put("cargo_price", orderParam.getCargoPrice() / 100.0)
                        .put("is_prepay", 0)
                        .put("receiver_name", orderParam.getReceiverName())
                        .put("receiver_address", orderParam.getReceiverAddress())
                        .put("receiver_lat", converted.getFirst())
                        .put("receiver_lng", converted.getSecond())
                        .put("callback", orderParam.getBackUrl())
                        .put("receiver_phone", orderParam.getReceiverPhone())
                        .put("receiver_tel", orderParam.getReceiverTel())
                        .put("tips", 0.0)
                        .put("info", orderParam.getInfo())
                        .put("cargo_type", 9)
                        .put("cargo_num", orderParam.getCargoNum())
                        .put("origin_mark", orderParam.getOriginMark())
                        .put("origin_mark_no", orderParam.getOriginMarkNo())
                        .put("cargo_weight", orderParam.getCargoWeight())
                        .build()
        ));
        return dadaDeliverHelper.post(this.jsonConverter.apply(data), api);
    }

    /**
     * 下单
     *
     * @param orderParam 订单参数
     * @param isGcj02    是否为高德坐标系
     * @return JSON String
     * @throws IOException
     */
    public String deliver(OrderParam orderParam, boolean isGcj02) throws IOException {
        return this.send(orderParam, DadaServerApi.API_ADD_ORDER, isGcj02);
    }

    /**
     * 重下单
     *
     * @param orderParam 订单参数
     * @param isGcj02    是否为高德坐标系
     * @return JSON String
     * @throws IOException
     */
    public String redeliver(OrderParam orderParam, boolean isGcj02) throws IOException {
        return this.send(orderParam, DadaServerApi.API_RE_ADD_ORDER, isGcj02);
    }

    /**
     * 运费查询
     *
     * @param orderParam 订单参数
     * @param isGcj02    是否为高德坐标系
     * @return JSON String
     * @throws IOException
     */
    public String freight(OrderParam orderParam, boolean isGcj02) throws IOException {
        return this.send(orderParam, DadaServerApi.API_ORDER_QUERY_DELIVER_FEE, isGcj02);
    }


    /**
     * 投诉
     *
     * @param orderId  订单ID
     * @param reasonId 投诉原因ID
     * @return JSON String
     */
    public String complain(String orderId, int reasonId) throws IOException {
        Map<String, Object> data = this.dadaDeliverHelper.sign(this.jsonConverter.apply(Maps.of(
                "order_id", orderId, "reason_id", reasonId
        )));
        return dadaDeliverHelper.post(this.jsonConverter.apply(data), DadaServerApi.API_COMPLAINT_DADA);
    }

    /**
     * 投诉原因列表
     *
     * @return JSON String
     * @throws IOException
     */
    public String complainReasons() throws IOException {
        Map<String, Object> data = this.dadaDeliverHelper.sign("");
        return dadaDeliverHelper.post(this.jsonConverter.apply(data), DadaServerApi.API_COMPLAINT_REASONS);
    }


    /**********************模拟配送端*******************************************************/
    /**
     * 模拟配送端发起操作
     * 接收
     *
     * @param orderId
     * @return
     */

    public String accept(String orderId) {

        try {
            // 初始化数据
            Map<String, Object> map = new HashMap<>();
            map.put("order_id", orderId);
            Map<String, Object> data = this.dadaDeliverHelper.sign(this.jsonConverter.apply(map));
            // 发送
            return dadaDeliverHelper.post(this.jsonConverter.apply(data), DadaServerApi.API_ACCEPT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 模拟配送端发起操作
     * 取货
     *
     * @param orderId
     * @return
     */
    public String fetch(String orderId) {

        try {
            // 初始化数据
            Map<String, Object> map = new HashMap<>();
            map.put("order_id", orderId);
            Map<String, Object> data = this.dadaDeliverHelper.sign(this.jsonConverter.apply(map));
            // 发送
            return dadaDeliverHelper.post(this.jsonConverter.apply(data), DadaServerApi.API_FETCH);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 模拟配送端发起操作
     * 完成配送
     *
     * @param orderId
     * @return
     */
    public String finish(String orderId) {

        try {
            // 初始化数据
            Map<String, Object> map = new HashMap<>();
            map.put("order_id", orderId);
            Map<String, Object> data = this.dadaDeliverHelper.sign(this.jsonConverter.apply(map));
            // 发送
            return dadaDeliverHelper.post(this.jsonConverter.apply(data), DadaServerApi.API_FINISH);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 模拟配送端发起操作
     * 取消
     *
     * @param orderId
     * @return
     */
    public String cancel(String orderId) {

        try {
            // 初始化数据
            Map<String, Object> map = new HashMap<>();
            map.put("order_id", orderId);
            Map<String, Object> data = this.dadaDeliverHelper.sign(this.jsonConverter.apply(map));
            // 发送
            return dadaDeliverHelper.post(this.jsonConverter.apply(data), DadaServerApi.API_CANCEL);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 模拟配送端发起操作
     * 过期
     * @param orderId
     * @return
     */
    public String expire(String orderId) {

        try {
            // 初始化数据
            Map<String, Object> map = new HashMap<>();
            map.put("order_id", orderId);
            Map<String, Object> data = this.dadaDeliverHelper.sign(this.jsonConverter.apply(map));
            // 发送
            return dadaDeliverHelper.post(this.jsonConverter.apply(data), DadaServerApi.API_EXPIRE);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**********************模拟配送端*******************************************************/


}
