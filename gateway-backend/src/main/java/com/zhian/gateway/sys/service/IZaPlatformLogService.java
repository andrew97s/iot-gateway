package com.zhian.gateway.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhian.gateway.sys.domain.ZaPlatformLog;

import java.util.List;

/**
 * 插件日志服务
 *
 * @author tongwenjin
 * @since 2026/5/12
 */
public interface IZaPlatformLogService extends IService<ZaPlatformLog> {

    /**
     * 按平台主键分页查询（需在调用前 {@code startPage()}）
     */
    List<ZaPlatformLog> selectByPlatformId(Long platformId);

    void recordByCode(String platformCode, int type, String title, String content);

    void recordByPlatformId(Long platformId, int type, String title, String content);

    /**
     * 清空某平台下全部运行日志
     */
    boolean clearByPlatformId(Long platformId);
}
