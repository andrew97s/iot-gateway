package com.zhian.gateway.sys.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhian.gateway.common.core.cache.Cache;
import com.zhian.gateway.common.exception.ServiceException;
import com.zhian.gateway.common.utils.DateUtils;
import com.zhian.gateway.common.utils.uuid.SnowflakeIdWorker;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.sys.mapper.ZaSysPlatformMapper;
import com.zhian.gateway.sys.service.IZaSysPlatformService;
import com.zhian.gateway.third.ThirdApplicationRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * 平台信息Service业务层处理
 * 
 * @author yepanpan
 * @date 2024-04-11
 */
@Service
public class ZaSysPlatformServiceImpl extends ServiceImpl<ZaSysPlatformMapper , ZaSysPlatform> implements IZaSysPlatformService
{
    private static  final  String CACHE_MAP = "za_sys_platform";
    @Autowired
    private Cache cache;

    @Autowired
    private ZaSysPlatformMapper zaSysPlatformMapper;

    /**
     * 缓存
     * @return
     */
    @PostConstruct
    public int reCache(){
        cache.deleteObject(CACHE_MAP);
        List<ZaSysPlatform> list = zaSysPlatformMapper.selectZaSysPlatformList(new ZaSysPlatform());
        list.forEach(t->{
            cache.setCacheMapValue(CACHE_MAP, "id_"+t.getId(), t);
            cache.setCacheMapValue(CACHE_MAP, "code_"+t.getCode(), t.getId());
        });
        return list.size();
    }

    /**
     * 查询平台信息
     *
     * @param code 平台代码
     * @return 平台信息
     */
    @Override
    public ZaSysPlatform selectZaSysPlatformByCode(String code){
        Long id = cache.getCacheMapValue(CACHE_MAP, "code_"+code);
        if(id !=null){
            return selectZaSysPlatformById(id);
        }else{
            return zaSysPlatformMapper.selectZaSysPlatformByCode(code);
        }
    }

    /**
     * 查询平台信息
     * 
     * @param id 平台信息主键
     * @return 平台信息
     */
    @Override
    public ZaSysPlatform selectZaSysPlatformById(Long id)
    {
        ZaSysPlatform zaSysPlatform = cache.getCacheMapValue(CACHE_MAP, "id_"+id);
        if(zaSysPlatform != null){
            return zaSysPlatform;
        }

        return zaSysPlatformMapper.selectZaSysPlatformById(id);
    }

    /**
     * 查询平台信息列表
     * 
     * @param zaSysPlatform 平台信息
     * @return 平台信息
     */
    @Override
    public List<ZaSysPlatform> selectZaSysPlatformList(ZaSysPlatform zaSysPlatform)
    {
        return zaSysPlatformMapper.selectZaSysPlatformList(zaSysPlatform);
    }

    /**
     * 新增平台信息
     * 
     * @param zaSysPlatform 平台信息
     * @return 结果
     */
    @Override
    public int insertZaSysPlatform(ZaSysPlatform zaSysPlatform)
    {
        zaSysPlatform.setId(SnowflakeIdWorker.getInstance().nextId());
        zaSysPlatform.setCreateTime(DateUtils.getNowDate());
        return zaSysPlatformMapper.insertZaSysPlatform(zaSysPlatform) + reCache();
    }

    /**
     * 修改平台信息
     * 
     * @param zaSysPlatform 平台信息
     * @return 结果
     */
    @Override
    public int updateZaSysPlatform(ZaSysPlatform zaSysPlatform)
    {
        zaSysPlatform.setUpdateTime(DateUtils.getNowDate());
        return zaSysPlatformMapper.updateZaSysPlatform(zaSysPlatform) + reCache();
    }

    /**
     * 批量删除平台信息
     * 
     * @param ids 需要删除的平台信息主键
     * @return 结果
     */
    @Override
    public int deleteZaSysPlatformByIds(Long[] ids)
    {
        return zaSysPlatformMapper.deleteZaSysPlatformByIds(ids) + reCache();
    }

    /**
     * 删除平台信息信息
     * 
     * @param id 平台信息主键
     * @return 结果
     */
    @Override
    public int deleteZaSysPlatformById(Long id)
    {
        return zaSysPlatformMapper.deleteZaSysPlatformById(id) + reCache();
    }

    /**
     * 启动/停止平台对接
     *
     * @param code 平台代码
     * @param start true启动/false停止
     * @return 结果
     */
    @Override
    public int triggerPlatform(String code, boolean start)
    {
        ZaSysPlatform zaSysPlatform = zaSysPlatformMapper.selectZaSysPlatformByCode(code);
        if(zaSysPlatform == null){
            throw new ServiceException("非法请求，平台不存在");
        }

        zaSysPlatform.setStatus(start ? "1" : "0");
        int rows = updateZaSysPlatform(zaSysPlatform);
        if(start){
            ThirdApplicationRunner.start(zaSysPlatform, ThirdApplicationRunner.START_REASON_MANUAL);
        }else{
            ThirdApplicationRunner.stop(zaSysPlatform);
        }
        return rows;
    }
}
