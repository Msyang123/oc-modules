package com.lhiot.oc.delivery.client.fengniao.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * 请求订单数据封装
 */
@ToString
public class ElemeCreateOrderRequest extends AbstractRequest {

    @Data
    @ToString
    @EqualsAndHashCode(callSuper = true)
    public static class ElemeCreateRequestData extends AbstractRequestData {
        /**
         * 第三方平台备注
         */
        @JsonProperty("partner_remark")
        private String partnerRemark;

        /**
         * 回调url
         */
        @JsonProperty("notify_url")
        private String notifyUrl;
        /**
         * 订单类型
         */
        @JsonProperty("order_type")
        private int orderType;

        /**
         * 门店编号
         */
        @JsonProperty("chain_store_code")
        private String chainStoreCode;
        /**
         * 配送点信息
         */
        @JsonProperty("transport_info")
        private TransportInfo transportInfo;
        /**
         * 订单总金额（不包含商家的任何活动以及折扣的金额）
         */
        @JsonProperty("order_total_amount")
        private BigDecimal orderTotalAmount;
        /**
         * 客户需要支付的金额
         */
        @JsonProperty("order_actual_amount")
        private BigDecimal orderActualAmount;
        /**
         * 用户备注
         */
        @JsonProperty("order_remark")
        private String orderRemark;
        /**
         * 是否需要发票
         */
        @JsonProperty("is_invoiced")
        private Integer ifNeedInvoiced;
        /**
         * 发票抬头, 如果需要发票 此项必填
         */
        @JsonProperty("invoice")
        private String invoice;
        /**
         * 订单支付状态 0：未支付 1：已支付
         */
        @JsonProperty("order_payment_status")
        private Integer orderPaymentStatus;
        /**
         * 订单支付方式 1：在线支付
         */
        @JsonProperty("order_payment_method")
        private Integer orderPaymentMethod;
        /**
         * 是否需要ele代收 0：否 1：是
         */
        @JsonProperty("is_agent_payment")
        private Integer ifNeedAgentPayment;
        /**
         * 需要代收时客户应付金额, 如需代收款 此项必填
         */
        @JsonProperty("require_payment_pay")
        private BigDecimal requirePaymentPay = new BigDecimal(0.0);    //传个默认0.0
        /**
         * 订单货物件数
         */
        @JsonProperty("goods_count")
        private Integer goodsCount;
        /**
         * 需要送达时间
         */
        @JsonProperty("require_receive_time")
        @JsonSerialize(using = ToStringSerializer.class)
        private long requireReceiveTime;
        /**
         * 下单时间
         */
        @JsonProperty("order_add_time")
        @JsonSerialize(using = ToStringSerializer.class)
        private long orderAddTime;
        /**
         * 订单重量
         */
        @JsonProperty("order_weight")
        private BigDecimal orderWeight=new BigDecimal(0);

        /**
         * 收货人信息
         */
        @JsonProperty("receiver_info")
        private ReceiverInfo receiverInfo;
        /**
         *
         */
        @JsonProperty("items_json")
        private ItemsJson[] itemsJson;

    }

    /**
     * 配送点信息
     */
    @Data
    @ToString
    public static class TransportInfo {

        //门店名称（支持汉字、符号、字母的组合），后期此参数将预留另用
        @JsonProperty("transport_name")
        private String name;

        //取货点地址，后期此参数将预留另用
        @JsonProperty("transport_address")
        private String address;

        //取货点经度，取值范围0～180，后期此参数将预留另用
        @JsonProperty("transport_longitude")
        private Double longitude;

        //取货点纬度，取值范围0～90，后期此参数将预留另用
        @JsonProperty("transport_latitude")
        private Double latitude; //

        //取货点经纬度来源, 1:腾讯地图, 2:百度地图, 3:高德地图
        @JsonProperty("position_source")
        private Integer positionSource;

        //取货点联系方式, 只支持手机号,400开头电话以及座机号码
        @JsonProperty("transport_tel")
        private String tel;

        //取货点备注
        @JsonProperty("transport_remark")
        private String remark;
    }

    /**
     * 收货人信息
     */
    @Data
    @ToString
    public static class ReceiverInfo {
        /**
         * 收货人姓名
         */
        @JsonProperty("receiver_name")
        private String name;
        /**
         * 收货人联系电话
         */
        @JsonProperty("receiver_primary_phone")
        private String primaryPhone;
        /**
         * 收货人备用电话
         */
        @JsonProperty("receiver_second_phone")
        private String secondPhone;
        /**
         * 收货人地址
         */
        @JsonProperty("receiver_address")
        private String address;
        /**
         * 收货人城市编码
         */
        @JsonProperty("receiver_city_code")
        private String cityCode;
        /**
         * 收货人城市
         */
        @JsonProperty("receiver_city_name")
        private String cityName;
        /**
         * 收货人经度
         */
        @JsonProperty("receiver_longitude")
        private BigDecimal longitude;
        /**
         * 收货人纬度
         */
        @JsonProperty("receiver_latitude")
        private BigDecimal latitude;

        /**
         * 经纬度来源
         */
        @JsonProperty("position_source")
        private int positionSource;

    }

    /**
     * 商品信息
     */
    @Data
    @ToString
    public static class ItemsJson {
        /**
         * 商品名称
         */
        @JsonProperty("item_name")
        private String name;
        /**
         * 商品数量
         */
        @JsonProperty("item_quantity")
        private int quantity;
        /**
         * 商品原价
         */
        @JsonProperty("item_price")
        private BigDecimal price;
        /**
         * 商品实际支付金额
         */
        @JsonProperty("item_actual_price")
        private BigDecimal actualPrice;
        /**
         * 是否需要ele打包 0：否 1：是
         */
        @JsonProperty("is_need_package")
        private Integer ifNeedPackage;
        /**
         * 是否代购 0：否 1：是
         */
        @JsonProperty("is_agent_purchase")
        private Integer ifNeedAgentPurchase;
        
        /**
         * 代购进价, 如果需要代购 此项必填
         */
        @JsonProperty("agent_purchase_price")
        private BigDecimal agentPurchasePrice;

    }
}
