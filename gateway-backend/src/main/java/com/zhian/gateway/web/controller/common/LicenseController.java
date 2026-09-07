package com.zhian.gateway.web.controller.common;

import cn.hutool.core.io.IoUtil;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.utils.file.FileUtils;
import com.zhian.gateway.framework.license.CustomLicenseService;
import com.zhian.gateway.framework.license.LicenseManagerHolder;
import com.zhian.gateway.framework.license.vo.LicenseInfo;
import de.schlichtherle.license.LicenseContent;
import de.schlichtherle.license.LicenseManager;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

import static com.zhian.gateway.common.core.domain.R.success;

/**
 * 证书相关controller
 *
 * @author tongwenjin
 * @since 2024-9-3
 */
@RestController
@RequestMapping("/license")
public class LicenseController {

    @Autowired
    private CustomLicenseService customLicenseService;

    @RequestMapping("/getInfo")
    @ApiOperation("获取证书信息")
    public R getLicenseInfo() {
        try {
            LicenseManager licenseManager = LicenseManagerHolder.getInstance(null);
            LicenseContent licenseContent = licenseManager.verify();

            LicenseInfo info = new LicenseInfo();
            info.setDescription(licenseContent.getInfo());
            info.setExpireTime(licenseContent.getNotAfter());
            info.setIssueTime(licenseContent.getIssued());
            return R.ok(info);
        }
        catch (Exception e) {
            return R.ok(null);
        }
    }

    /**
     * 获取服务器硬件信息
     *
     * @param osName 操作系统类型，如果为空则自动判断
     * @author SunHeng
     * @since 1.0.0
     */
    @SuppressWarnings("CallToPrintStackTrace")
    @RequestMapping(value = "/exportServerInfos")
    @ApiOperation("导出服务器信息")
    public void getServerInfos(
            @RequestParam(value = "osName", required = false) String osName ,
            HttpServletResponse response
    ) {
        try {
            // 服务器数据
            String machineId = customLicenseService.getServerInfoStr(osName);
            // 文件响应
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            FileUtils.setAttachmentResponseHeader(response, "server.txt");
            IoUtil.write(response.getOutputStream(),false, machineId.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("导出服务器信息失败，请联系管理员！");
        }
    }

    @PostMapping("/importLicense")
    @ApiOperation("导入授权证书")
    public R importData(@RequestParam MultipartFile file) throws Exception {
        // 重新安装证书
        customLicenseService.reInstall(file);
        return success("导入成功");
    }
}
