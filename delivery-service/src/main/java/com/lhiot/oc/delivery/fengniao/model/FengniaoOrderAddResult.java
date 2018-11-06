package com.lhiot.oc.delivery.fengniao.model;

import lombok.Data;

// 正确返回结果
//{
//        "code":200,
//        "msg":"接收成功",
//        "data":{}
//        }
//  }


/**
 * 蜂鸟配送添加订单正确返回结果对象
 */
@Data
public class FengniaoOrderAddResult {

    private int code;

    private String msg;

    private String data;

}
