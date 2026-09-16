package com.zhian.gateway.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 读取项目相关配置
 *
 * @author zhian
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "zhian")
public class ZhianConfig
{
    /** 项目名称 */
    private String name;

    /** 版本 */
    private String version;

    /** 版权年份 */
    private String copyrightYear;

    /** 实例演示开关 */
    private boolean demoEnabled;

    /**
     * 数据库类型 mysql 、 sqlite
     */
    private String dbType;

    /** 上传路径 */
    @Getter
    private static String basePath;

    /** 上传路径 */
    @Getter
    private static String profile;

    /** 获取地址开关 */
    @Getter
    private static boolean addressEnabled;

    /** 验证码类型 */
    @Getter
    private static String captchaType;
    /**
     * 获取导入上传路径
     */
    public static String getImportPath()
    {
        return getProfile() + "/import";
    }

    public void setBasePath(String basePath) {
        ZhianConfig.basePath = basePath;
    }

    public void setProfile(String profile) {
        ZhianConfig.profile = profile;
    }

    public void setAddressEnabled(Boolean addressEnabled) {
        ZhianConfig.addressEnabled = addressEnabled;
    }

    public void setCaptchaType(String captchaType) {
        ZhianConfig.captchaType = captchaType;
    }

    /**
     * 获取头像上传路径
     */
    public static String getAvatarPath()
    {
        return getProfile() + "/avatar";
    }

    /**
     * 获取下载路径
     */
    public static String getDownloadPath()
    {
        return getProfile() + "/download/";
    }

    /**
     * 获取上传路径
     */
    public static String getUploadPath()
    {
        return getProfile() + "/upload";
    }
}
