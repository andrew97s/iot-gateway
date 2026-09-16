package com.zhian.gateway.task;

import com.github.pagehelper.PageHelper;
import com.zhian.gateway.common.core.cache.Cache;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.sys.domain.ZaSysError;
import com.zhian.gateway.sys.service.IZaSysErrorService;
import com.zhian.gateway.third.ThirdApplicationRunner;
import com.zhian.gateway.third.ThirdHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("platformTask")
@Slf4j
public class PlatformTask {
    public static final String ERROR_MQ_MAP = "error_mq";
    @Autowired
    private IZaSysErrorService zaSysErrorService;
    @Autowired
    private Cache cache;

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
