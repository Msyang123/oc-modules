package com.lhiot.oc.delivery.client.dada;

import com.leon.microx.util.Maps;
import com.leon.microx.util.Pair;
import com.leon.microx.util.Position;
import com.lhiot.oc.delivery.client.dada.model.OrderParam;
import com.lhiot.oc.delivery.client.dada.model.ShopParam;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

/**
 * @author Leon (234239150@qq.com) created in 12:21 18.9.18
 */
@Slf4j
public class DadaClient {

    private Helper helper;

    private Function<Map<String, Object>, String> converter;

    public DadaClient(Config config, Function<Map<String, Object>, String> converter) {
        this.helper = new Helper(config);
        this.converter = converter;
    }

    /**
     * 取消订单<br/>
     * 在订单待接单或待取货情况下，调用此接口可取消订单。注意：接单后1－15分钟内取消订单，运费退回。同时扣除2元作为给配送员的违约金
     *
     * @param orderId        订单ID
     * @param cancelReasonId 取消原因ID
     * @param cancelReason   取消原因
     * @return JSON String
     * @throws IOException Http Request
     */
    public String cancel(String orderId, long cancelReasonId, String cancelReason) throws IOException {
        Map<String, Object> data = this.helper.sign(this.converter.apply(
                Maps.of("order_id", orderId, "cancel_reason_id", cancelReasonId, "cancel_reason", cancelReason)
        ));
        return this.helper.post(this.converter.apply(data), DadaApi.API_ORDER_FORMAL_CANCEL);
    }

    /**
     * 退单原因列表
     *
     * @return JSON String
     * @throws IOException Http Request
     */
    public String cancelReasons() throws IOException {
        Map<String, Object> data = this.helper.sign("");
        return this.helper.post(this.converter.apply(data), DadaApi.API_ORDER_CANCEL_REASONS);
    }

    /**
     * 订单详情 [状态] 查询
     *
     * @param orderId 订单ID
     * @return JSON String
     * @throws IOException Http Request
     */
    public String one(String orderId) throws IOException {
        Map<String, Object> data = this.helper.sign(this.converter.apply(Maps.of("order_id", orderId)));
        return this.helper.post(this.converter.apply(data), DadaApi.API_ORDER_STATUS_QUERY);
    }

    /*
     * 下单
     *
     * @param orderParam 下单参数
     * @param isGcj02    是否需要转成高德系标准 百度坐标系需要，腾讯坐标系不需要
     * @return JSON String
     * @throws IOException Http Request
     */
    private String send(OrderParam orderParam, String api, boolean isGcj02) throws IOException {
        Position.Coordinate converted = this.helper.convertCoordinates(orderParam.getLng(), orderParam.getLat(), isGcj02);
        Map<String, Object> data = this.helper.sign(this.converter.apply(
                Maps.<String, Object>builder()
                        .put("shop_no", orderParam.getShopNo())
                        .put("origin_id", orderParam.getOriginId())
                        .put("city_code", orderParam.getCityCode())
                        .put("cargo_price", orderParam.getCargoPrice() / 100.0)
                        .put("is_prepay", 0)
                        .put("receiver_name", orderParam.getReceiverName())
                        .put("receiver_address", orderParam.getReceiverAddress())
                        .put("receiver_lat", converted.getLatitude())
                        .put("receiver_lng", converted.getLongitude())
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
        return this.helper.post(this.converter.apply(data), api);
    }

    /**
     * 下单
     *
     * @param orderParam 订单参数
     * @param isGcj02    是否为高德坐标系
     * @return JSON String
     * @throws IOException Http Request
     */
    public String deliver(OrderParam orderParam, boolean isGcj02) throws IOException {
        return this.send(orderParam, DadaApi.API_ADD_ORDER, isGcj02);
    }

    /**
     * 重下单
     *
     * @param orderParam 订单参数
     * @param isGcj02    是否为高德坐标系
     * @return JSON String
     * @throws IOException Http Request
     */
    public String redeliver(OrderParam orderParam, boolean isGcj02) throws IOException {
        return this.send(orderParam, DadaApi.API_RE_ADD_ORDER, isGcj02);
    }

    /**
     * 运费查询
     *
     * @param orderParam 订单参数
     * @param isGcj02    是否为高德坐标系
     * @return JSON String
     * @throws IOException Http Request
     */
    public String freight(OrderParam orderParam, boolean isGcj02) throws IOException {
        return this.send(orderParam, DadaApi.API_ORDER_QUERY_DELIVER_FEE, isGcj02);
    }

    /**
     * @param clientId   客户端ID
     * @param orderId    订单ID
     * @param updateTime 修改时间戳
     * @param signature  签名串
     * @return 验签是否通过
     */
    public boolean inspect(Object clientId, Object orderId, Object updateTime, Object signature){
        return this.helper.inspect(clientId,orderId,updateTime,signature);
    }


    /**
     * 投诉
     *
     * @param orderId  订单ID
     * @param reasonId 投诉原因ID
     * @return JSON String
     */
    public String complain(String orderId, int reasonId) throws IOException {
        Map<String, Object> data = this.helper.sign(this.converter.apply(Maps.of(
                "order_id", orderId, "reason_id", reasonId
        )));
        return this.helper.post(this.converter.apply(data), DadaApi.API_COMPLAINT_DADA);
    }

    /**
     * 投诉原因列表
     *
     * @return JSON String
     * @throws IOException Http Request
     */
    public String complainReasons() throws IOException {
        Map<String, Object> data = this.helper.sign("");
        return this.helper.post(this.converter.apply(data), DadaApi.API_COMPLAINT_REASONS);
    }


    /**
     * 门店详情
     *
     * @param originShopId 门店ID
     * @return JSON String
     * @throws IOException
     */
    public String detail(String originShopId) throws IOException {
        Map<String, Object> data = this.helper.sign(this.converter.apply(Maps.of("origin_shop_id", originShopId)));
        return helper.post(this.converter.apply(data), DadaApi.API_SHOP_DETAIL);
    }

    /**
     * 修改门店信息
     *
     * @param shop      门店
     * @param newShopId 新门店编号
     * @param status    门店状态（1-门店激活，0-门店下线）
     * @param isGcj02   是否为高德系标准 如果不是，将自动转换。（百度坐标系需要转换，腾讯坐标系不用转换）
     * @return JSON String
     * @throws IOException
     */
    public String update(ShopParam shop, String newShopId, int status, boolean isGcj02) throws IOException {
        Position.Coordinate converted = this.helper.convertCoordinates(shop.getLng(), shop.getLat(), isGcj02);
        Map<String, Object> map = Maps.<String, Object>builder()
                .put("origin_shop_id", shop.getOriginShopId()).put("new_shop_id", newShopId)
                .put("station_name", shop.getStationName()).put("business", 9)
                .put("city_name", shop.getCityName()).put("area_name", shop.getAreaName())
                .put("station_address", shop.getStationAddress())
                .put("lat", converted.getLatitude()).put("lng", converted.getLongitude())
                .put("contact_name", shop.getContactName())
                .put("phone", shop.getPhone()).put("status", status)// 门店状态（1-门店激活，0-门店下线）
                .build();
        Map<String, Object> data = this.helper.sign(this.converter.apply(map));
        return helper.post(this.converter.apply(data), DadaApi.API_UPDATE_SHOP);
    }

    /**
     * 添加门店
     *
     * @param shop    门店信息
     * @param isGcj02 是否为高德系标准 如果不是，将自动转换。（百度坐标系需要转换，腾讯坐标系不用转换）
     * @return JSON String
     * @throws IOException
     */
    public String add(ShopParam shop, Function<List<Map<String, Object>>, String> jsonArrayConverter, boolean isGcj02) throws IOException {
        Position.Coordinate converted = this.helper.convertCoordinates(shop.getLng(), shop.getLat(), isGcj02);
        Map<String, Object> map = Maps.<String, Object>builder()
                .put("station_name", shop.getStationName()).put("origin_shop_id", shop.getOriginShopId())
                .put("city_name", shop.getCityName()).put("area_name", shop.getAreaName()).put("station_address", shop.getStationAddress())
                .put("contact_name", shop.getContactName()).put("business", 9)
                .put("lat", converted.getLatitude()).put("lng", converted.getLongitude())
                .put("phone", shop.getPhone()).build();
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(map);
        Map<String, Object> data = this.helper.sign(jsonArrayConverter.apply(list));
        return helper.post(this.converter.apply(data), DadaApi.API_ADD_SHOP);
    }

    /**
     * 获取城市列表
     *
     * @return JSON String
     * @throws IOException
     */
    public String citys() throws IOException {
        Map<String, Object> data = this.helper.sign("");
        return helper.post(this.converter.apply(data), DadaApi.API_CITY_CODE_LIST);
    }

    /**********************模拟配送端*******************************************************/

    /**
     *
     *  模拟配送端发起操作
     *   接收
     * @param hdOrderCode 订单code
     * @return String
     */
    public String query(String hdOrderCode){
        try {
            // 初始化数据
            Map<String, Object> map = new HashMap<>();
            map.put("order_id", hdOrderCode);
            Map<String, Object> data = this.helper.sign(this.converter.apply(map));
            // 发送
            return this.helper.post(this.converter.apply(data), DadaApi.API_ORDER_STATUS_QUERY);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * 模拟配送端发起操作
     * 接收
     *
     * @param hdOrderCode 订单ID
     * @return json
     */
    public String accept(String hdOrderCode) {

        try {
            // 初始化数据
            Map<String, Object> map = new HashMap<>();
            map.put("order_id", hdOrderCode);
            Map<String, Object> data = this.helper.sign(this.converter.apply(map));
            // 发送
            return this.helper.post(this.converter.apply(data), DadaApi.API_ACCEPT);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * 模拟配送端发起操作
     * 取货
     *
     * @param hdOrderCode 订单ID
     * @return json
     */
    public String fetch(String hdOrderCode) {

        try {
            // 初始化数据
            Map<String, Object> map = new HashMap<>();
            map.put("order_id", hdOrderCode);
            Map<String, Object> data = this.helper.sign(this.converter.apply(map));
            // 发送
            return this.helper.post(this.converter.apply(data), DadaApi.API_FETCH);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * 模拟配送端发起操作
     * 完成配送
     *
     * @param hdOrderCode 订单ID
     * @return json
     */
    public String finish(String hdOrderCode) {

        try {
            // 初始化数据
            Map<String, Object> map = new HashMap<>();
            map.put("order_id", hdOrderCode);
            Map<String, Object> data = this.helper.sign(this.converter.apply(map));
            // 发送
            return this.helper.post(this.converter.apply(data), DadaApi.API_FINISH);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * 模拟配送端发起操作
     * 取消
     *
     * @param hdOrderCode 订单ID
     * @return json
     */
    public String cancel(String hdOrderCode) {

        try {
            // 初始化数据
            Map<String, Object> map = new HashMap<>();
            map.put("order_id", hdOrderCode);
            Map<String, Object> data = this.helper.sign(this.converter.apply(map));
            // 发送
            return this.helper.post(this.converter.apply(data), DadaApi.API_CANCEL);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * 模拟配送端发起操作
     * 过期
     *
     * @param orderId 订单ID
     * @return json
     */
    public String expire(String orderId) {

        try {
            // 初始化数据
            Map<String, Object> map = new HashMap<>();
            map.put("order_id", orderId);
            Map<String, Object> data = this.helper.sign(this.converter.apply(map));
            // 发送
            return this.helper.post(this.converter.apply(data), DadaApi.API_EXPIRE);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**********************模拟配送端*******************************************************/

    static class Helper {

        private Config config;

        Helper(Config config) {
            this.config = config;
        }

        /**
         * 签名
         *
         * @param jsonString JSON参数
         * @return 签名后的Map
         */
        Map<String, Object> sign(String jsonString) {
            SortedMap<String, Object> map = (SortedMap<String, Object>) Maps.<String, Object>as(new TreeMap<>())
                    .put("format", config.getFormat())
                    .put("timestamp", Instant.now().toEpochMilli())
                    .put("app_key", config.getAppKey())
                    .put("v", config.getVersion())
                    .put("source_id", config.getSourceId())
                    .put("body", jsonString).build();
            StringBuilder s = new StringBuilder();
            map.forEach((key, value) -> s.append(key).append(value));
            map.put("signature", md5(config.getAppSecret() + s + config.getAppSecret(), true));
            return map;
        }

        /**
         * 验签（远端签名后，本地验签）
         *
         * @param clientId   客户端ID
         * @param orderId    订单ID
         * @param updateTime 修改时间戳
         * @param signature  签名串
         * @return 验签是否通过
         */
        public boolean inspect(Object clientId, Object orderId, Object updateTime, Object signature) {
            //第一步：将参与签名的字段的值进行升序排列
            List<Object> list = new ArrayList<>();
            list.add(clientId);
            list.add(orderId);
            list.add(updateTime);
            StringBuilder signStr = new StringBuilder();
            list.stream().sorted().forEach(signStr::append);
            //第二步：将排序过后的参数，进行字符串拼接
            String sign = md5(signStr.toString(), false);
            return (signature.equals(sign));
        }

        public String post(String jsonString, String uri) throws IOException {
            URL url = new URL(config.getUrl() + uri);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.addRequestProperty("Accept", "application/json");
            conn.addRequestProperty("Content-Type", "application/json;charset=" + config.getCharset());
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonString.getBytes(config.getCharset()));
                os.flush();
            }

            StringBuilder result = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    result.append(line);
                }
            }
            return result.toString();
        }

        private static final String DIGITS = "0123456789abcdef";

        private String md5(final String str, boolean upperCase) {
            char[] digest = upperCase ? DIGITS.toUpperCase().toCharArray() : DIGITS.toLowerCase().toCharArray();
            char[] out;
            try {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                md5.update(str.getBytes(this.config.getCharset()));
                byte[] data = md5.digest();

                final int l = data.length;
                out = new char[l << 1];
                for (int i = 0, j = 0; i < l; i++) {
                    out[j++] = digest[(0xF0 & data[i]) >>> 4];
                    out[j++] = digest[0x0F & data[i]];
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return new String(out);
        }

        /**
         * 坐标系转换
         *
         * @param lng     经度
         * @param lat     纬度
         * @param isGcj02 是否为高德坐标系（如果不是高德坐标系，则需要转换。例如百度坐标系）
         * @return {@link Pair} first=lat  second=lng
         */
        Position.Coordinate convertCoordinates(double lng, double lat, boolean isGcj02) {
            if (isGcj02) {
                return Position.base(lng, lat);
            }
            Position.BD09 bd09 = Position.baidu(lng, lat);
            return Position.GCJ02.of(bd09);
        }
    }

    @Data
    public static class Config {

        private String appKey;
        private String appSecret;
        private String url;

        private String sourceId;
        private String version;
        private String format = "json";
        private String charset = "UTF-8";
        private String backUrl;//回调第三方服务地址
    }
}
