package com.zhian.gateway.task;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhian.gateway.sys.domain.ZaSysError;
import com.zhian.gateway.sys.domain.ZaSysMessage;
import com.zhian.gateway.sys.mapper.ZaSysErrorMapper;
import com.zhian.gateway.sys.mapper.ZaSysMessageMapper;
import com.zhian.gateway.system.domain.SysConfig;
import com.zhian.gateway.system.mapper.SysConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Date;

/**
 * 数据清除任务
 *
 * @author tongwenjin
 * @since 2024-8-15
 */

@Component
@Slf4j
public class DataTruncateTask {

    @Autowired
    private ZaSysMessageMapper messageMapper;

    @Autowired
    private ZaSysErrorMapper errorMapper;

    @Autowired
    private SysConfigMapper configMapper;

    @Value("${spring.datasource.druid.master.url}")
    private String jdbcUrl;

    /**
     * 每日凌晨一点执行一次
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void truncateData() {
        // 获取数据保留期限配置值
        SysConfig conf = configMapper.selectOne(
                Wrappers.lambdaUpdate(SysConfig.class)
                        .eq(SysConfig::getConfigKey, "data_preserved_day_count")
                        .last("limit 1")
        );
        int dayCount = conf != null ? Integer.parseInt(conf.getConfigValue()) : 30;
        // 清除监测相关数据 (za_sys_message , za_sys_error)
        int messageCount = messageMapper.delete(
                Wrappers.lambdaUpdate(ZaSysMessage.class)
                        .lt(ZaSysMessage::getCreateTime, DateUtil.offsetDay(new Date(), -dayCount))
        );
        log.info(" 成功从 za_sys_message 表清除共 {} 条数据!", messageCount);
        int errorCount = errorMapper.delete(
                Wrappers.lambdaUpdate(ZaSysError.class)
                        .lt(ZaSysError::getLogTime, DateUtil.offsetDay(new Date(), -dayCount))
        );
        log.info(" 成功从 za_sys_error 表清除共 {} 条数据!", errorCount);

        // 尝试清除缓存（释放已删除文件磁盘占用）
        try {
            Connection conn = DriverManager.getConnection(jdbcUrl);
            Statement stmt = conn.createStatement();
            stmt.execute("VACUUM");
            log.info("vacuum 成功 !");
        } catch (Exception e) {
            log.error("vacuum 失败: {}", e.getMessage());
        }
    }
}
