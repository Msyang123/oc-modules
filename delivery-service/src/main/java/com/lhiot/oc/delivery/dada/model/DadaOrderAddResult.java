package com.lhiot.oc.delivery.dada.model;

import lombok.Data;

// 正确返回结果
//      "status": "success",
//      "result": {
//          "distance": 53459.98,
//          "fee": 51.0
//          "deliverFee": 51.0
//      },
//      "code": 0,
//      "msg": "成功"
//  }
/**
 * 达达配送添加订单正确返回结果对象
 */
@Data
public class DadaOrderAddResult {
    private String status;

    private int code;

    private String msg;

    private Result result;

    @Data
    public class Result {
        private double distance;
        private double fee;
        private double deliverFee;
    }
}