package com.lhiot.oc.payment.feign;

import lombok.Data;
import lombok.ToString;

/**
 * @author Leon (234239150@qq.com) created in 13:28 18.11.28
 */
@Data
@ToString
public class User {
    private Long id;
    private String phone;
    private String realName;
    private String idCard;
    private Long balance;
    private Long memberPoints;
}
