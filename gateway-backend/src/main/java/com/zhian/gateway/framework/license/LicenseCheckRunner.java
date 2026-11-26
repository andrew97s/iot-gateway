package com.zhian.gateway.framework.license;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 在项目启动时安装证书
 *
 * @author sunHeng
 * @since 1.0.0
 */
@Slf4j
@Component
public class LicenseCheckRunner implements CommandLineRunner {

    @Autowired
    private CustomLicenseService customLicenseService;

    @Override
    public void run(String... args) throws Exception {
        log.info("++++++++ 开始安装证书 ++++++++");
        customLicenseService.installLicense();
        log.info("++++++++ 证书安装结束 ++++++++");
    }

}
