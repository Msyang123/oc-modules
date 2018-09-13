package com.lhiot.oc.basic.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DeliverTimeItem {
    private String display;
    private String startTime;
    private String endTime;
    //{"display":"立即配送","startTime":"2017-01-02 12:33:34","endTime":"2017-01-02 12:33:34"}
}
