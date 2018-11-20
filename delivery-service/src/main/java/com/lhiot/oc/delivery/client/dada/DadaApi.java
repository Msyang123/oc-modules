package com.lhiot.oc.delivery.client.dada;

/**
 * @author liuyo on 17.8.5.
 */
public interface DadaApi {
    String API_ADD_SHOP = "/api/shop/add";

    String API_UPDATE_SHOP = "/api/shop/update";

    String API_SHOP_DETAIL = "/api/shop/detail";

    String API_ADD_ORDER = "/api/order/addOrder";

    String API_RE_ADD_ORDER = "/api/order/reAddOrder";

    String API_ORDER_FORMAL_CANCEL = "/api/order/formalCancel";

    String API_ORDER_QUERY_DELIVER_FEE = "/api/order/queryDeliverFee";

    String API_ORDER_STATUS_QUERY = "/api/order/status/query";

    String API_ORDER_CANCEL_REASONS = "/api/order/cancel/reasons";

    String API_CITY_CODE_LIST = "/api/cityCode/list";

    String API_COMPLAINT_DADA = "/api/complaint/dada";

    String API_COMPLAINT_REASONS = "/api/complaint/reasons";
    
    String API_ACCEPT="/api/order/accept";
    
    String API_FETCH= "/api/order/fetch";
    
    String API_FINISH= "/api/order/finish";
    
    String API_CANCEL= "/api/order/cancel";
    
    String API_EXPIRE= "/api/order/expire";
}
