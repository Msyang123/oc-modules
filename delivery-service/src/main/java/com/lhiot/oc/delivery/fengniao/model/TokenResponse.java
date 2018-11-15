package com.lhiot.oc.delivery.fengniao.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.ToString;

/**
 * token响应类
 */
@ToString
public class TokenResponse extends AbstractResponse {

    private TokenData data;

    @JsonCreator
    public TokenResponse(@JsonProperty("data") TokenData data) {
        this.data = data;
    }

    @JsonIgnoreProperties({"app_id"})
    @Data
    @ToString
    public static class TokenData {
        @JsonProperty("access_token")
        private String accessToken;
        @JsonProperty("expire_time")
        @JsonSerialize(using = ToStringSerializer.class)
        private long expireTime;

        @JsonCreator
        public TokenData(@JsonProperty("access_token") String accessToken, @JsonProperty("expire_time") long expireTime) {
            this.accessToken = accessToken;
            this.expireTime = expireTime;
        }
    }

    public TokenData getData() {
        return data;
    }

    public void setData(TokenData data) {
        this.data = data;
    }

}
