package com.zhian.gateway.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhian.gateway.sys.domain.ZaSysPlatform;

import java.util.List;

/**
 * 平台信息Mapper接口
 * 
 * @author yepanpan
 * @date 2024-04-11
 */
public interface ZaSysPlatformMapper extends BaseMapper<ZaSysPlatform>
{
    /**
     * 查询平台信息
     * 
     * @param code 平台代码
     * @return 平台信息
     */
    public ZaSysPlatform selectZaSysPlatformByCode(String code);

    /**
     * 查询平台信息
     *
     * @param id 平台信息主键
     * @return 平台信息
     */
    public ZaSysPlatform selectZaSysPlatformById(Long id);

    /**
     * 查询平台信息列表
     * 
     * @param zaSysPlatform 平台信息
     * @return 平台信息集合
     */
    public List<ZaSysPlatform> selectZaSysPlatformList(ZaSysPlatform zaSysPlatform);

    /**
     * 新增平台信息
     * 
     * @param zaSysPlatform 平台信息
     * @return 结果
     */
    public int insertZaSysPlatform(ZaSysPlatform zaSysPlatform);

    /**
     * 修改平台信息
     * 
     * @param zaSysPlatform 平台信息
     * @return 结果
     */
    public int updateZaSysPlatform(ZaSysPlatform zaSysPlatform);

    /**
     * 删除平台信息
     * 
     * @param id 平台信息主键
     * @return 结果
     */
    public int deleteZaSysPlatformById(Long id);

    /**
     * 批量删除平台信息
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteZaSysPlatformByIds(Long[] ids);
}
