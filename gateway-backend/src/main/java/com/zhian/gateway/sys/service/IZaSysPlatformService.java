package com.zhian.gateway.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhian.gateway.sys.domain.ZaSysPlatform;

import java.util.List;

/**
 * 平台信息Service接口
 * 
 * @author yepanpan
 * @date 2024-04-11
 */
public interface IZaSysPlatformService extends IService<ZaSysPlatform>
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
     * 批量删除平台信息
     * 
     * @param ids 需要删除的平台信息主键集合
     * @return 结果
     */
    public int deleteZaSysPlatformByIds(Long[] ids);

    /**
     * 删除平台信息信息
     * 
     * @param id 平台信息主键
     * @return 结果
     */
    public int deleteZaSysPlatformById(Long id);


    /**
     * 启动/停止平台对接
     *
     * @param code 平台代码
     * @param start true启动/false停止
     * @return 结果
     */
    public int triggerPlatform(String code, boolean start);
}
