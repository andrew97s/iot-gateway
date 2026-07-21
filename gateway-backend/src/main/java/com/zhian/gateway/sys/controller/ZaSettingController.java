package com.zhian.gateway.sys.controller;

import com.zhian.gateway.common.core.controller.BaseController;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.system.domain.SysConfig;
import com.zhian.gateway.system.service.ISysConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 网关核心参数配置
 *
 * 统一管理网关级参数（存储于 sys_config），供「系统配置-核心参数」页读写。
 * 仅允许操作 {@link #DEFAULTS} 中声明的键，避免任意配置写入。
 */
@Api("网关核心参数")
@RestController
@RequestMapping("/sys/setting")
public class ZaSettingController extends BaseController {

    /** 允许管理的参数键及默认值 */
    private static final Map<String, String[]> DEFAULTS = new LinkedHashMap<>();

    static {
        // key -> {默认值, 配置名称}
        DEFAULTS.put("gateway.name",              new String[]{"物联网关", "网关名称"});
        DEFAULTS.put("gateway.code",              new String[]{"GW-0001", "网关编码（同步上级平台时的网关标识）"});
        DEFAULTS.put("data_preserved_day_count",  new String[]{"30", "消息/错误数据保留天数"});
        DEFAULTS.put("gateway.offline.threshold", new String[]{"300", "设备离线判定阈值（秒）"});
        DEFAULTS.put("gateway.plugin.autorestart", new String[]{"true", "插件异常自动重启"});
        DEFAULTS.put("gateway.device.autosync",   new String[]{"false", "新设备自动同步上级平台"});
    }

    @Autowired
    private ISysConfigService configService;

    @ApiOperation("查询核心参数")
    @GetMapping("/params")
    public R getParams() {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> e : DEFAULTS.entrySet()) {
            String value = configService.selectConfigByKey(e.getKey());
            result.put(e.getKey(), StringUtils.isEmpty(value) ? e.getValue()[0] : value);
        }
        return success(result);
    }

    @ApiOperation("保存核心参数")
    @PreAuthorize("@ss.hasPermi('system:config:edit')")
    @PutMapping("/params")
    public R saveParams(@RequestBody Map<String, String> params) {
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (!DEFAULTS.containsKey(e.getKey()) || e.getValue() == null) {
                continue;
            }
            upsert(e.getKey(), e.getValue(), DEFAULTS.get(e.getKey())[1]);
        }
        return success();
    }

    private void upsert(String key, String value, String name) {
        SysConfig query = new SysConfig();
        query.setConfigKey(key);
        List<SysConfig> exists = configService.selectConfigList(query);
        if (exists != null && !exists.isEmpty()) {
            SysConfig config = exists.get(0);
            config.setConfigValue(value);
            config.setUpdateBy(getUsername());
            configService.updateConfig(config);
        } else {
            SysConfig config = new SysConfig();
            config.setConfigName(name);
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setConfigType("N");
            config.setCreateBy(getUsername());
            configService.insertConfig(config);
        }
    }
}
