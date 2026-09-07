package com.zhian.gateway.sys.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.common.utils.uuid.SnowflakeIdWorker;
import com.zhian.gateway.sys.domain.ZaPlatformLog;
import com.zhian.gateway.sys.domain.ZaSysError;
import com.zhian.gateway.sys.mapper.ZaSysErrorMapper;
import com.zhian.gateway.sys.service.IZaPlatformLogService;
import com.zhian.gateway.sys.service.IZaSysErrorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 错误日志Service业务层处理
 * 
 * @author yepanpan
 * @date 2024-04-11
 */
@Service
public class ZaSysErrorServiceImpl extends ServiceImpl<ZaSysErrorMapper , ZaSysError> implements IZaSysErrorService
{
    @Autowired
    private ZaSysErrorMapper zaSysErrorMapper;
    @Autowired
    private IZaPlatformLogService zaPlatformLogService;

    /**
     * 查询错误日志
     * 
     * @param id 错误日志主键
     * @return 错误日志
     */
    @Override
    public ZaSysError selectZaSysErrorById(Long id)
    {
        return zaSysErrorMapper.selectZaSysErrorById(id);
    }

    /**
     * 查询错误日志列表
     * 
     * @param zaSysError 错误日志
     * @return 错误日志
     */
    @Override
    public List<ZaSysError> selectZaSysErrorList(ZaSysError zaSysError)
    {
        return zaSysErrorMapper.selectZaSysErrorList(zaSysError);
    }


    /**
     * 快速记录错误日志
     */
    @Override
    public int log(String type, String title, String error, String content){
        return logWithPlatform(null, type, title, error, content);
    }

    /**
     * 快速记录关联平台的错误/事件日志
     */
    @Override
    public int logWithPlatform(String pfCode, String type, String title, String error, String content){
        String safeTitle = title == null ? "无标题" : (title.length() < 200 ? title : title.substring(0,198));
        ZaSysError zaSysError = new ZaSysError();
        zaSysError.setPfCode(pfCode);
        zaSysError.setType(type);
        zaSysError.setTitle(safeTitle);
        if(error != null) {
            zaSysError.setError(error.length() < 200 ? error : error.substring(0, 198));
        }
        if(content != null) {
            zaSysError.setContent(content.length() > 500 ? content.substring(0, 498): content);
        }
        int rows = insertZaSysError(zaSysError);
        if (rows > 0 && StringUtils.isNotEmpty(pfCode)) {
            int logType = ZaSysError.TYPE_PLATFORM_EVENT.equals(type)
                    ? ZaPlatformLog.TYPE_OTHER
                    : ZaPlatformLog.TYPE_ERROR;
            StringBuilder sb = new StringBuilder();
            if (StringUtils.isNotEmpty(error)) {
                sb.append(error);
            }
            if (StringUtils.isNotEmpty(content)) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(content);
            }
            zaPlatformLogService.recordByCode(pfCode, logType, safeTitle,
                    sb.length() > 0 ? sb.toString() : null);
        }
        return rows;
    }

    /**
     * 查询指定平台的最近日志
     */
    @Override
    public List<ZaSysError> selectByPlatform(String pfCode, int limit){
        return zaSysErrorMapper.selectByPlatform(pfCode, limit);
    }

    @Override
    public void purge() {
        zaSysErrorMapper.purge();
    }

    /**
     * 新增错误日志
     * 
     * @param zaSysError 错误日志
     * @return 结果
     */
    @Override
    public int insertZaSysError(ZaSysError zaSysError)
    {
        zaSysError.setId(SnowflakeIdWorker.getInstance().nextId());
        if(zaSysError.getLogTime() == null){
            zaSysError.setLogTime(new Date());
        }
        return zaSysErrorMapper.insertZaSysError(zaSysError);
    }

    /**
     * 修改错误日志
     * 
     * @param zaSysError 错误日志
     * @return 结果
     */
    @Override
    public int updateZaSysError(ZaSysError zaSysError)
    {
        return zaSysErrorMapper.updateZaSysError(zaSysError);
    }

    /**
     * 批量删除错误日志
     * 
     * @param ids 需要删除的错误日志主键
     * @return 结果
     */
    @Override
    public int deleteZaSysErrorByIds(Long[] ids)
    {
        return zaSysErrorMapper.deleteZaSysErrorByIds(ids);
    }

    /**
     * 删除错误日志信息
     * 
     * @param id 错误日志主键
     * @return 结果
     */
    @Override
    public int deleteZaSysErrorById(Long id)
    {
        return zaSysErrorMapper.deleteZaSysErrorById(id);
    }
}
