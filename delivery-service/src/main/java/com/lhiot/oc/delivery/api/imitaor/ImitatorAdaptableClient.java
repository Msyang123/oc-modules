package com.lhiot.oc.delivery.api.imitaor;

import com.leon.microx.web.result.Tips;

/**
 * @author zhangfeng create in 12:03 2018/11/26
 */
public interface ImitatorAdaptableClient {

    Tips accept(String hdOrderCode);

    Tips fetch(String hdOrderCode);

    Tips finish(String hdOrderCode);

    Tips cancel(String hdOrderCode);

    Tips expire(String hdOrderCode);
}
