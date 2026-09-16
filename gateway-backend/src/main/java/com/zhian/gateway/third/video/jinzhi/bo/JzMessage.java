package com.zhian.gateway.third.video.jinzhi.bo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.websocket.Session;

/**
 * 金智业务消息对象
 *
 * @author tongwenjin
 * @since 2026/8/24
 */
@Data
@AllArgsConstructor
public class JzMessage {

    @JsonIgnore
    private Session session;

    private String message;
}
