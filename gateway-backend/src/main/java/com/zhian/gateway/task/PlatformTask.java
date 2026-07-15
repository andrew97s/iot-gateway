package com.zhian.gateway.task;

import com.github.pagehelper.PageHelper;
import com.zhian.gateway.common.core.cache.Cache;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.sys.domain.ZaSysError;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.sys.service.IZaSysErrorService;
import com.zhian.gateway.sys.service.IZaSysPlatformService;
import com.zhian.gateway.third.ThirdApplicationRunner;
import com.zhian.gateway.third.ThirdHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("platformTask")
@Slf4j
public class PlatformTask {
    public static final String ERROR_MQ_MAP = "error_mq";
    @Autowired
    private IZaSysPlatformService zaSysPlatformService;
    @Autowired
    private IZaSysErrorService zaSysErrorService;
    @Autowired
    private Cache cache;

    /**
     * 定时巡检各接入服务是否正常
     */
    @Scheduled(cron = "19 0/10 * * * ?")
    public void alive(){
        log.debug("定时巡检各接入服务是否正常");
        // 「系统配置-核心参数」可关闭插件异常自动重启
        try {
            String autoRestart = com.zhian.gateway.common.utils.spring.SpringUtils
                    .getBean(com.zhian.gateway.system.service.ISysConfigService.class)
                    .selectConfigByKey("gateway.plugin.autorestart");
            if ("false".equalsIgnoreCase(autoRestart)) {
                log.debug("插件异常自动重启已关闭，跳过巡检重启");
                return;
            }
        } catch (Exception ignore) {
            // 配置不可用时默认开启
        }
        ZaSysPlatform pc = new ZaSysPlatform();
        pc.setStatus("1");
        pc.setRunning(ZaSysPlatform.STATE_RUNNING);
        List<ZaSysPlatform> list = zaSysPlatformService.selectZaSysPlatformList(pc);
        for(ZaSysPlatform platform:list){
            ThirdHandler handler = ThirdApplicationRunner.getHandler(platform.getCode());
            if(handler == null || handler.isAlive()){
               continue;
            }

            // TODO 定期刷新插件接入的设备状态

            //自动启动一次
            if(!handler.start(platform)) {
                zaSysErrorService.logWithPlatform(platform.getCode(), ZaSysError.TYPE_API_TIMEOUT,
                        platform.getName() + "对接服务重启失败", "对接异常停止", null);

                platform.setRunning(ZaSysPlatform.STATE_STOP);
                zaSysPlatformService.updateZaSysPlatform(platform);
            }
        }
    }

    /**
     * 重复处理未处置成功的消息，最多5次
     */
    public void reprocess(){
        ZaSysError ec = new ZaSysError();
        ec.setTitle(ZaSysError.TYPE_MQ);
        PageHelper.startPage(1, 10, "log_time desc");
        List<ZaSysError> list = zaSysErrorService.selectZaSysErrorList(ec);
        if(StringUtils.isEmpty(list)){
            return;
        }

        list.forEach(e->{
            Integer times = cache.getCacheMapValue(ERROR_MQ_MAP, e.getId().toString());
            try {
                ThirdHandler handler = ThirdApplicationRunner.getHandler(e.getTitle());
                if (handler != null) {
                    handler.processMsg(e.getContent());
                }
                zaSysErrorService.deleteZaSysErrorById(e.getId());
                if(times != null){
                    cache.deleteCacheMapValue(ERROR_MQ_MAP, e.getId().toString());
                }
            }catch (Exception ex){
                if(times == null || times > 5){
                    zaSysErrorService.deleteZaSysErrorById(e.getId());
                    cache.deleteCacheMapValue(ERROR_MQ_MAP, e.getId().toString());
                }else{
                    cache.setCacheMapValue(ERROR_MQ_MAP, e.getId().toString(), times == null ? 1 : times+1);
                    log.error("{}消息处理5次失败:{}", e.getTitle(), e.getContent());
                }
                ex.printStackTrace();
            }
        });
    }
}
