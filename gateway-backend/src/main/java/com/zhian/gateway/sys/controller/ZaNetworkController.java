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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 网关网络 / IP 配置
 *
 * 1. 实时枚举本机网卡与地址信息（只读）。
 * 2. 期望网络配置（静态IP/DHCP等）保存到 sys_config（key: gateway.network.config），
 *    实际应用到宿主机需结合部署环境（netplan / nmcli / 脚本）执行，本接口负责持久化配置意图。
 */
@Api("网络配置")
@RestController
@RequestMapping("/sys/network")
public class ZaNetworkController extends BaseController {

    /** 网络配置在 sys_config 中的键 */
    public static final String CONFIG_KEY = "gateway.network.config";

    @Autowired
    private ISysConfigService configService;

    @ApiOperation("枚举本机网卡")
    @GetMapping("/interfaces")
    public R interfaces() {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (nic.isLoopback() || nic.isVirtual() || !nic.isUp()) {
                    continue;
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", nic.getName());
                item.put("displayName", nic.getDisplayName());
                item.put("up", nic.isUp());
                item.put("mac", formatMac(nic.getHardwareAddress()));
                List<Map<String, Object>> addrs = new ArrayList<>();
                for (InterfaceAddress ia : nic.getInterfaceAddresses()) {
                    InetAddress addr = ia.getAddress();
                    if (!(addr instanceof Inet4Address)) {
                        continue;
                    }
                    Map<String, Object> a = new LinkedHashMap<>();
                    a.put("ip", addr.getHostAddress());
                    a.put("prefixLength", ia.getNetworkPrefixLength());
                    a.put("netmask", prefixToNetmask(ia.getNetworkPrefixLength()));
                    a.put("broadcast", ia.getBroadcast() == null ? null : ia.getBroadcast().getHostAddress());
                    addrs.add(a);
                }
                item.put("addresses", addrs);
                result.add(item);
            }
        } catch (Exception e) {
            return error("枚举网卡失败：" + e.getMessage());
        }
        return success(result);
    }

    @ApiOperation("查询网络配置意图")
    @GetMapping("/config")
    public R getConfig() {
        String value = configService.selectConfigByKey(CONFIG_KEY);
        return success(StringUtils.isEmpty(value) ? "{}" : value);
    }

    @ApiOperation("保存网络配置意图")
    @PreAuthorize("@ss.hasPermi('system:config:edit')")
    @PutMapping("/config")
    public R saveConfig(@RequestBody String configJson) {
        SysConfig query = new SysConfig();
        query.setConfigKey(CONFIG_KEY);
        List<SysConfig> exists = configService.selectConfigList(query);
        if (exists != null && !exists.isEmpty()) {
            SysConfig config = exists.get(0);
            config.setConfigValue(configJson);
            config.setUpdateBy(getUsername());
            configService.updateConfig(config);
        } else {
            SysConfig config = new SysConfig();
            config.setConfigName("网关网络配置");
            config.setConfigKey(CONFIG_KEY);
            config.setConfigValue(configJson);
            config.setConfigType("N");
            config.setCreateBy(getUsername());
            configService.insertConfig(config);
        }
        return success();
    }

    private static String formatMac(byte[] mac) {
        if (mac == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            sb.append(String.format("%02X%s", mac[i], i < mac.length - 1 ? ":" : ""));
        }
        return sb.toString();
    }

    private static String prefixToNetmask(int prefix) {
        int mask = prefix == 0 ? 0 : 0xffffffff << (32 - prefix);
        return String.format("%d.%d.%d.%d",
                (mask >>> 24) & 0xff, (mask >>> 16) & 0xff, (mask >>> 8) & 0xff, mask & 0xff);
    }
}
