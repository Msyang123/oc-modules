/*
 * create by leon
 * 写字楼里写字间,写字间里程序员;
 * 程序人员写程序,又拿程序换酒钱;
 * 酒醒只在网上坐,酒醉还来网下眠;
 * 酒醉酒醒日复日,网上网下年复年;
 * 但愿老死电脑间,不愿鞠躬老板前;
 * 奔驰宝马贵者趣,公交自行程序员;
 * 别人笑我忒疯癫,我笑自己命太贱;
 * 不见满街漂亮妹,哪个归得程序员.
 */
package com.lhiot.oc.basic.service.refund;

import com.leon.microx.common.wrapper.Tips;
import com.lhiot.oc.basic.domain.BaseOrderInfo;
import com.lhiot.oc.basic.domain.inparam.ReturnOrderParam;
import com.lhiot.order.domain.BaseOrderInfo;
import com.lhiot.order.domain.inparam.ReturnOrderParam;

/**
 * 专门用来执行普通订单退货的接口
 *
 * @author liuyo on 17.8.24.
 */
public interface IOrderRefund {
//	HdOrderReduceUtil hdUtil = new HdOrderReduceUtil();

    /**
     * 海鼎退货
     *
     * @param data                   退货参数
     * @return 退货记录条数，提示信息
     * @throws Exception
     */
    Tips doRefund(BaseOrderInfo normalOrder,
                  ReturnOrderParam data) throws Exception;

    /*enum Message {
        ORDER_NOT_FOUND(-1, "未找到订单信息"),
        ORDER_BARCODE_IDS_IS_BLANK(-1, "传递订单规格编号不能为空！"),
        ACTIVITY_GWFL_REFUND_FAILURE(-1, "购物返利的订单退款失败"),
        FAILURE(-1, "退货失败"),
        SUCCESS(1, "success");

        private int code;
        private String value;

        Message(int code, String value) {
            this.code = code;
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public int getCode() {
            return code;
        }
    }*/
}
