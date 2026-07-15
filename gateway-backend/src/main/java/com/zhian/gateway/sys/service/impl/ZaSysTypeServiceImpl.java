package com.zhian.gateway.sys.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhian.gateway.common.core.cache.Cache;
import com.zhian.gateway.common.utils.DateUtils;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.common.utils.uuid.SnowflakeIdWorker;
import com.zhian.gateway.sys.domain.ZaSysType;
import com.zhian.gateway.sys.mapper.ZaSysTypeMapper;
import com.zhian.gateway.sys.service.IZaSysTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * 设备类型Service业务层处理
 * 
 * @author yepanpan
 * @date 2024-04-11
 */
@Service
public class ZaSysTypeServiceImpl
        extends ServiceImpl<ZaSysTypeMapper , ZaSysType> implements IZaSysTypeService
{
    private static  final  String CACHE_MAP = "za_sys_type";
    @Autowired
    private Cache cache;

    @Autowired
    private ZaSysTypeMapper zaSysTypeMapper;

    /**
     * 缓存
     * @return
     */
    @PostConstruct
    public int reCache(){
        cache.deleteObject(CACHE_MAP);
        List<ZaSysType> list = zaSysTypeMapper.selectZaSysTypeList(new ZaSysType());
        list.forEach(t->{
            cache.setCacheMapValue(CACHE_MAP, "id_"+t.getId(), t);
            cache.setCacheMapValue(CACHE_MAP, "code_"+t.getCode(), t.getId());
            if(StringUtils.isNotEmpty(t.getJbCode())){
                cache.setCacheMapValue(CACHE_MAP, "jb_"+t.getJbCode(), t.getId());
            }
        });
        return list.size();
    }

    /**
     * 查询设备类型
     *
     * @param jbCode 青鸟设备类型代码
     * @return 设备类型
     */
    @Override
    public ZaSysType selectZaSysTypeByJbCode(String jbCode){
        Long id = cache.getCacheMapValue(CACHE_MAP, "jb_"+jbCode);
        if(id == null){
            return zaSysTypeMapper.selectZaSysTypeByJbCode(jbCode);
        }else {
            return zaSysTypeMapper.selectZaSysTypeById(id);
        }
    }

    /**
     * 查询设备类型
     *
     * @param code 设备类型代码
     * @return 设备类型
     */
    @Override
    public ZaSysType selectZaSysTypeByCode(String code){
        Long id = cache.getCacheMapValue(CACHE_MAP, "code_"+code);
        if(id == null){
            return zaSysTypeMapper.selectZaSysTypeByCode(code);
        }else {
            return zaSysTypeMapper.selectZaSysTypeById(id);
        }
    }

    /**
     * 查询设备类型
     *
     * @param name 设备类型名称
     * @return 设备类型
     */
    @Override
    public ZaSysType selectZaSysTypeByName(String name){
        return zaSysTypeMapper.selectZaSysTypeByName(name);
    }

    /**
     * 查询设备类型
     * 
     * @param id 设备类型主键
     * @return 设备类型
     */
    @Override
    public ZaSysType selectZaSysTypeById(Long id)
    {
        return zaSysTypeMapper.selectZaSysTypeById(id);
    }

    /**
     * 查询设备类型列表
     * 
     * @param zaSysType 设备类型
     * @return 设备类型
     */
    @Override
    public List<ZaSysType> selectZaSysTypeList(ZaSysType zaSysType)
    {
        return zaSysTypeMapper.selectZaSysTypeList(zaSysType);
    }

    /**
     * 新增设备类型
     * 
     * @param zaSysType 设备类型
     * @return 结果
     */
    @Override
    public int insertZaSysType(ZaSysType zaSysType)
    {
        zaSysType.setId(SnowflakeIdWorker.getInstance().nextId());
        zaSysType.setCreateTime(DateUtils.getNowDate());
        return zaSysTypeMapper.insertZaSysType(zaSysType) + reCache();
    }

    /**
     * 修改设备类型
     * 
     * @param zaSysType 设备类型
     * @return 结果
     */
    @Override
    public int updateZaSysType(ZaSysType zaSysType)
    {
        zaSysType.setUpdateTime(DateUtils.getNowDate());
        return zaSysTypeMapper.updateZaSysType(zaSysType) + reCache();
    }

    /**
     * 批量删除设备类型
     * 
     * @param ids 需要删除的设备类型主键
     * @return 结果
     */
    @Override
    public int deleteZaSysTypeByIds(Long[] ids)
    {
        return zaSysTypeMapper.deleteZaSysTypeByIds(ids) + reCache();
    }

    /**
     * 删除设备类型信息
     * 
     * @param id 设备类型主键
     * @return 结果
     */
    @Override
    public int deleteZaSysTypeById(Long id)
    {
        return zaSysTypeMapper.deleteZaSysTypeById(id) + reCache();
    }
}
