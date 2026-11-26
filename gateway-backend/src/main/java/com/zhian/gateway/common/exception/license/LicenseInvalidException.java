package com.zhian.gateway.common.exception.license;

/**
 * 无效证书异常
 *
 * @author tongwenjin
 * @since 2024-8-20
 */
public class LicenseInvalidException extends RuntimeException{

    public LicenseInvalidException(String message) {
        super(message);
    }
}
