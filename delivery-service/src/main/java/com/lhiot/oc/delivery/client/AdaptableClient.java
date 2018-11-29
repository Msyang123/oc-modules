package com.lhiot.oc.delivery.client;

import com.leon.microx.util.Position;
import com.leon.microx.web.result.Tips;
import com.lhiot.oc.delivery.entity.DeliverNote;
import com.lhiot.oc.delivery.feign.Store;
import com.lhiot.oc.delivery.model.CancelReason;
import com.lhiot.oc.delivery.model.CoordinateSystem;
import com.lhiot.oc.delivery.model.DeliverOrder;
import org.springframework.lang.Nullable;

import java.util.Map;

/**
 * 可以被适配的配送端抽象
 */
public interface AdaptableClient {

    /**
     * 发送配送单
     *
     * @param coordinate    坐标系
     * @param store         发货门店
     * @param deliverOrder  配送单
     * @param deliverNoteId 数据库配送单ID（先存库再发配送）
     * @return Tips
     */
    Tips send(CoordinateSystem coordinate, Store store, DeliverOrder deliverOrder, @Nullable Long deliverNoteId);

    /**
     * 取消配送
     *
     * @param deliverNote 配送单
     * @param reason      取消原因
     * @return Tips
     */
    Tips cancel(DeliverNote deliverNote, CancelReason reason);

    /**
     * 获取取消配送原因列表
     *
     * @return Tips
     */
    Tips cancelReasons();

    /**
     * 获取配送单详情
     *
     * @param deliverNote 配送单
     * @return tips
     */
    Tips deliverNoteDetail(DeliverNote deliverNote);

    default double distance(Store store, DeliverOrder deliverOrder, CoordinateSystem coordinate) {
        Position.Coordinate storeCoordinate = Position.base(store.getLongitude().doubleValue(), store.getLatitude().doubleValue());
        Position.Coordinate deliverCoordinate;
        if (coordinate.isNeedConvert()) {
            Position.BD09 bd09 = Position.baidu(deliverOrder.getLng(), deliverOrder.getLat());
            deliverCoordinate = Position.GCJ02.of(bd09);
        } else {
            deliverCoordinate = Position.base(deliverOrder.getLng(), deliverOrder.getLat());
        }
        return storeCoordinate.distance(deliverCoordinate).doubleValue();
    }

    /**
     * 回调验签
     *
     * @param backParam 验签参数列表
     * @return
     */
    Tips backSignature(Map<String, String> backParam);
}
