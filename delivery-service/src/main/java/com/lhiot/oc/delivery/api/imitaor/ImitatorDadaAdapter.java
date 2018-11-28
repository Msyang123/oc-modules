package com.lhiot.oc.delivery.api.imitaor;

import com.leon.microx.web.result.Tips;
import com.lhiot.oc.delivery.client.dada.DadaClient;

/**
 * @author zhangfeng create in 12:11 2018/11/26
 */
public class ImitatorDadaAdapter implements ImitatorAdaptableClient {

    private DadaClient client;

    public ImitatorDadaAdapter(DadaClient client) {
        this.client = client;
    }

    @Override
    public Tips accept(String hdOrderCode) {
        String result = client.accept(hdOrderCode);
        return Tips.info(result);
    }

    @Override
    public Tips fetch(String hdOrderCode) {
        String result = client.fetch(hdOrderCode);
        return Tips.info(result);
    }

    @Override
    public Tips finish(String hdOrderCode) {
        String result = client.finish(hdOrderCode);
        return Tips.info(result);
    }

    @Override
    public Tips cancel(String hdOrderCode) {
        String result = client.cancel(hdOrderCode);
        return Tips.info(result);
    }

    @Override
    public Tips expire(String hdOrderCode) {
        String result = client.expire(hdOrderCode);
        return Tips.info(result);
    }
}
