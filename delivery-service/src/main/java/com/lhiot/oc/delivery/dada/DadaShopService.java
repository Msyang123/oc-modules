package com.lhiot.oc.delivery.dada;

import com.leon.microx.util.Maps;
import com.leon.microx.util.Pair;
import com.lhiot.oc.delivery.dada.model.ShopParam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Leon (234239150@qq.com) created in 13:11 18.9.18
 */
public class DadaShopService {
    private DadaDeliverHelper dadaDeliverHelper;

    private Function<Map<String, Object>, String> jsonConverter;

    public DadaShopService(DadaDeliverHelper dadaDeliverHelperHelper, Function<Map<String, Object>, String> jsonConverter) {
        this.dadaDeliverHelper = dadaDeliverHelperHelper;
        this.jsonConverter = jsonConverter;
    }

    /**
     * 门店详情
     *
     * @param originShopId 门店ID
     * @return JSON String
     * @throws IOException
     */
    public String detail(String originShopId) throws IOException {
        Map<String, Object> data = this.dadaDeliverHelper.sign(this.jsonConverter.apply(Maps.of("origin_shop_id", originShopId)));
        return dadaDeliverHelper.post(this.jsonConverter.apply(data), DadaServerApi.API_SHOP_DETAIL);
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
        Pair<Double, Double> converted = this.dadaDeliverHelper.convertCoordinates(shop.getLng(), shop.getLat(), isGcj02);
        Map<String, Object> map = Maps.<String, Object>builder()
                .put("origin_shop_id", shop.getOriginShopId()).put("new_shop_id", newShopId)
                .put("station_name", shop.getStationName()).put("business", 9)
                .put("city_name", shop.getCityName()).put("area_name", shop.getAreaName())
                .put("station_address", shop.getStationAddress())
                .put("lat", converted.getFirst()).put("lng", converted.getSecond())
                .put("contact_name", shop.getContactName())
                .put("phone", shop.getPhone()).put("status", status)// 门店状态（1-门店激活，0-门店下线）
                .build();
        Map<String, Object> data = this.dadaDeliverHelper.sign(this.jsonConverter.apply(map));
        return dadaDeliverHelper.post(this.jsonConverter.apply(data), DadaServerApi.API_UPDATE_SHOP);
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
        Pair<Double, Double> converted = this.dadaDeliverHelper.convertCoordinates(shop.getLng(), shop.getLat(), isGcj02);
        Map<String, Object> map = Maps.<String, Object>builder()
                .put("station_name", shop.getStationName()).put("origin_shop_id", shop.getOriginShopId())
                .put("city_name", shop.getCityName()).put("area_name", shop.getAreaName()).put("station_address", shop.getStationAddress())
                .put("contact_name", shop.getContactName()).put("business", 9)
                .put("lat", converted.getFirst()).put("lng", converted.getSecond())
                .put("phone", shop.getPhone()).build();
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(map);
        Map<String, Object> data = this.dadaDeliverHelper.sign(jsonArrayConverter.apply(list));
        return dadaDeliverHelper.post(this.jsonConverter.apply(data), DadaServerApi.API_ADD_SHOP);
    }

    /**
     * 获取城市列表
     *
     * @return JSON String
     * @throws IOException
     */
    public String citys() throws IOException {
        Map<String, Object> data = this.dadaDeliverHelper.sign("");
        return dadaDeliverHelper.post(this.jsonConverter.apply(data), DadaServerApi.API_CITY_CODE_LIST);
    }
}
