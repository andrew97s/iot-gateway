package com.zhian.gateway.third.video.roc.common;

import com.zhian.gateway.sys.domain.ZaSysDevice;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * ROC消息上下文
 *
 * @author tongwenjin
 * @since 2025-2-19
 */

@Data
public class RocContext {
    public static RocContext context = new RocContext();
    private RocContext(){};
    public static RocContext getInstance(){return context;}

    private String username = "admin";

    private String password = "123456";

    private String deviceName;

    private String serialNum;

    private String realm;

    private String nonce;

    private String cnonce = "client_nonce";

    private String qop;

    private Long sequence = 1L;

    private String faceRule;

    private Boolean faceEnable = false;

    private Boolean faceGroup = false;

    private ZaSysDevice device;

    private Set<String> faceSet = new HashSet<>();
}
