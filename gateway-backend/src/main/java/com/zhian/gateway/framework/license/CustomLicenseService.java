package com.zhian.gateway.framework.license;

import cn.hutool.core.io.FileUtil;
import com.zhian.gateway.common.exception.license.LicenseInvalidException;
import com.zhian.gateway.framework.license.impl.LinuxServerInfos;
import com.zhian.gateway.framework.license.impl.WindowsServerInfos;
import com.zhian.gateway.framework.license.properties.LicenseProperties;
import de.schlichtherle.license.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.prefs.Preferences;

/**
 * CustomLicenseService
 *
 * @author yangyixin
 * @since 2024 /03/08 0008
 */
@Slf4j
@Component
public class CustomLicenseService {

    @Autowired
    private LicenseProperties licenseProperties;

    /**
     * 重新安装证书
     *
     * @param file the file
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public synchronized void reInstall(MultipartFile file) throws Exception {
        File oldFile = new File(licenseProperties.getLicensePath());
        File bkFile = new File(licenseProperties.getLicensePath() + "-bk");

        if (FileUtil.exist(oldFile)) {
            // 备份历史证书
            FileUtil.move(oldFile, bkFile, true);
        }
        // 覆盖 & 安装
        try {
            FileUtil.writeFromStream(file.getInputStream(),oldFile);
            LicenseContent licenseContent = installLicense();
            if (licenseContent == null) {
                throw new LicenseInvalidException("证书无效,请联系管理员!");
            }
        }
        catch (Exception e) {
            log.info("安装新证书发生异常,msg:{}" , e.getMessage());
            e.printStackTrace();
            // 尝试回滚
            if (bkFile.exists()) {
                FileUtil.move(bkFile, oldFile, true);
                LicenseContent licenseContent = installLicense();
                if (licenseContent != null) {
                    log.info("证书已成功恢复!");
                }
            }
            throw new LicenseInvalidException("证书无效,请联系管理员!");
        }
    }

    /**
     * Install license license content.
     *
     * @param licensePath the license path
     * @return the license content
     */
    public synchronized LicenseContent installLicense(String licensePath) {
        if (StringUtils.isBlank(licensePath)) {
            return null;
        }
        LicenseContent result = null;
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        //1. 安装证书
        try {
            LicenseManager licenseManager = LicenseManagerHolder.getInstance(initLicenseParam());
            licenseManager.uninstall();

            result = licenseManager.install(new File(licenseProperties.getLicensePath()));
            log.info("证书安装成功，证书有效期：{}-{}", format.format(result.getNotBefore()), format.format(result.getNotAfter()));

        } catch (Exception e) {
            log.error("证书安装失败！", e);
        }

        return result;
    }

    /**
     * Install license license content.
     *
     * @return the license content
     */
    public synchronized LicenseContent installLicense() {
        return installLicense(licenseProperties.getLicensePath());
    }

    private LicenseParam initLicenseParam() {
        Preferences preferences = Preferences.userNodeForPackage(LicenseVerify.class);

        CipherParam cipherParam = new DefaultCipherParam(licenseProperties.getStorePass());

        KeyStoreParam publicStoreParam = new CustomKeyStoreParam(
                LicenseVerify.class,
                licenseProperties.getPublicKeysStorePath(),
                licenseProperties.getPublicAlias(),
                licenseProperties.getStorePass(),
                null);

        return new DefaultLicenseParam(licenseProperties.getSubject(), preferences, publicStoreParam, cipherParam);
    }

    public String getServerInfoStr(String osName) throws Exception {
        //操作系统类型
        if (StringUtils.isBlank(osName)) {
            osName = System.getProperty("os.name");
        }
        osName = osName.toLowerCase();
        AbstractServerInfos abstractServerInfos = null;
        //根据不同操作系统类型选择不同的数据获取方法
        if (osName.startsWith("windows")) {
            abstractServerInfos = new WindowsServerInfos();
        } else if (osName.startsWith("linux")) {
            abstractServerInfos = new LinuxServerInfos();
        } else {//其他服务器类型
            abstractServerInfos = new LinuxServerInfos();
        }

        // 返回cpuId + mainBoardId的MD5值代表对应的机器码
        String cpuId = abstractServerInfos.getCPUSerial();
        String mainBoardId = abstractServerInfos.getMainBoardSerial();

        return Base64.getEncoder().encodeToString((cpuId + "-" + mainBoardId).getBytes(StandardCharsets.UTF_8));
    }
}
