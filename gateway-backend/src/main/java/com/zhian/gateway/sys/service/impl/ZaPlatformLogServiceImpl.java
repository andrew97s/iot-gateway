package com.zhian.gateway.sys.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhian.gateway.common.utils.DateUtils;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.sys.domain.ZaPlatformLog;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.sys.mapper.ZaPlatformLogMapper;
import com.zhian.gateway.sys.service.IZaPlatformLogService;
import com.zhian.gateway.sys.service.IZaSysPlatformService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 插件日志默认实现类
 *
 * @author tongwenjin
 * @since 2026/5/12
 */
@Service
@Slf4j
public class ZaPlatformLogServiceImpl
        extends ServiceImpl<ZaPlatformLogMapper, ZaPlatformLog>
        implements IZaPlatformLogService {

    private static final int TITLE_MAX = 250;
    /** 与 MySQL TEXT 上限对齐，便于记录完整配置 JSON */
    private static final int CONTENT_MAX = 60000;

    @Autowired
    private IZaSysPlatformService zaSysPlatformService;

    @Override
    public List<ZaPlatformLog> selectByPlatformId(Long platformId) {
        if (platformId == null) {
            return Collections.emptyList();
        }
        return list(Wrappers.<ZaPlatformLog>lambdaQuery()
                .eq(ZaPlatformLog::getPlatformId, platformId)
                .orderByDesc(ZaPlatformLog::getCreateTime));
    }

    @Override
    public void recordByCode(String platformCode, int type, String title, String content) {
        if (StringUtils.isEmpty(platformCode)) {
            return;
        }
        ZaSysPlatform p = zaSysPlatformService.selectZaSysPlatformByCode(platformCode);
        if (p == null || p.getId() == null) {
            log.warn("recordByCode skip: platform not found code={}", platformCode);
            return;
        }
        recordByPlatformId(p.getId(), type, title, content);
    }

    @Override
    public void recordByPlatformId(Long platformId, int type, String title, String content) {
        if (platformId == null) {
            return;
        }
        try {
            ZaPlatformLog row = new ZaPlatformLog();
            row.setPlatformId(platformId);
            row.setType(type);
            row.setTitle(truncate(title, TITLE_MAX));
            row.setContent(truncate(content, CONTENT_MAX));
            row.setCreateTime(DateUtils.getNowDate());
            save(row);
        } catch (Exception e) {
            log.error("recordByPlatformId failed platformId={}", platformId, e);
        }
    }

    @Override
    public boolean clearByPlatformId(Long platformId) {
        if (platformId == null) {
            return false;
        }
        return remove(Wrappers.<ZaPlatformLog>lambdaQuery().eq(ZaPlatformLog::getPlatformId, platformId));
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max) + "\n...(内容过长已截断)";
    }
}
