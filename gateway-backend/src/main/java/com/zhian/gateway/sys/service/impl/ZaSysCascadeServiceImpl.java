package com.zhian.gateway.sys.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhian.gateway.common.core.cache.Cache;
import com.zhian.gateway.common.utils.DateUtils;
import com.zhian.gateway.common.utils.uuid.SnowflakeIdWorker;
import com.zhian.gateway.sys.domain.ZaSysCascade;
import com.zhian.gateway.sys.mapper.ZaSysCascadeMapper;
import com.zhian.gateway.sys.service.IZaSysCascadeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * 级联平台Service业务层处理
 * 
 * @author yepanpan
 * @date 2024-05-13
 */
@Service
public class ZaSysCascadeServiceImpl extends ServiceImpl<ZaSysCascadeMapper, ZaSysCascade> implements IZaSysCascadeService
{
    private static  final  String CACHE_MAP = "za_sys_cascade";
    @Autowired
    private Cache cache;
    @Autowired
    private ZaSysCascadeMapper zaSysCascadeMapper;

    /**
     * 缓存
     * @return
     */
    @PostConstruct
    public int reCache(){
        cache.deleteObject(CACHE_MAP);
        List<ZaSysCascade> list = zaSysCascadeMapper.selectZaSysCascadeList(new ZaSysCascade());
        list.forEach(t->{
            cache.setCacheMapValue(CACHE_MAP, "id_"+t.getId(), t);
            cache.setCacheMapValue(CACHE_MAP, "code_"+t.getCode(), t.getId());
        });
        return list.size();
    }

    /**
     * 查询级联平台
     *
     * @param code 级联平台代码
     * @return 级联平台
     */
    public ZaSysCascade selectZaSysCascadeByCode(String code){
        Long id = cache.getCacheMapValue(CACHE_MAP, "code_"+code);
        if(id != null){
            return cache.getCacheMapValue(CACHE_MAP, "id_"+id);
        }
        return zaSysCascadeMapper.selectZaSysCascadeByCode(code);
    }

    /**
     * 查询级联平台
     * 
     * @param id 级联平台主键
     * @return 级联平台
     */
    @Override
    public ZaSysCascade selectZaSysCascadeById(Long id)
    {
        ZaSysCascade zaSysCascade = cache.getCacheMapValue(CACHE_MAP, "id_"+id);
        if(zaSysCascade != null){
            return zaSysCascade;
        }
        return zaSysCascadeMapper.selectZaSysCascadeById(id);
    }

    /**
     * 查询级联平台列表
     * 
     * @param zaSysCascade 级联平台
     * @return 级联平台
     */
    @Override
    public List<ZaSysCascade> selectZaSysCascadeList(ZaSysCascade zaSysCascade)
    {
        return zaSysCascadeMapper.selectZaSysCascadeList(zaSysCascade);
    }

    /**
     * 新增级联平台
     * 
     * @param zaSysCascade 级联平台
     * @return 结果
     */
    @Override
    public int insertZaSysCascade(ZaSysCascade zaSysCascade)
    {
        zaSysCascade.setId(SnowflakeIdWorker.getInstance().nextId());
        zaSysCascade.setCreateTime(DateUtils.getNowDate());
        return zaSysCascadeMapper.insertZaSysCascade(zaSysCascade) + reCache();
    }

    /**
     * 修改级联平台
     * 
     * @param zaSysCascade 级联平台
     * @return 结果
     */
    @Override
    public int updateZaSysCascade(ZaSysCascade zaSysCascade)
    {
        zaSysCascade.setUpdateTime(DateUtils.getNowDate());
        return zaSysCascadeMapper.updateZaSysCascade(zaSysCascade) + reCache();
    }

    /**
     * 批量删除级联平台
     * 
     * @param ids 需要删除的级联平台主键
     * @return 结果
     */
    @Override
    public int deleteZaSysCascadeByIds(Long[] ids)
    {
        return zaSysCascadeMapper.deleteZaSysCascadeByIds(ids) + reCache();
    }

    /**
     * 删除级联平台信息
     * 
     * @param id 级联平台主键
     * @return 结果
     */
    @Override
    public int deleteZaSysCascadeById(Long id)
    {
        return zaSysCascadeMapper.deleteZaSysCascadeById(id) + reCache();
    }
}
