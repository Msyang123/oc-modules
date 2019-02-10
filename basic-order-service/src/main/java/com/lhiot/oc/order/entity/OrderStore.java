package com.lhiot.oc.order.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Date;

/**
 * @author zhangfeng created in 2018/9/18 15:29
 **/
@Data
public class OrderStore {
    private String hdOrderCode;
    private Long orderId;
    @NotNull
    private Long storeId;
    @NotNull
    private String storeCode;
    @NotNull
    private String storeName;
    private String operationUser;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createAt = Date.from(Instant.now());
}
