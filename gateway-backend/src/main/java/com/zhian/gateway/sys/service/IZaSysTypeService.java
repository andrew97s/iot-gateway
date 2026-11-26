package com.zhian.gateway.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhian.gateway.sys.domain.ZaSysType;

import java.util.List;

/**
 * 设备类型Service接口
 * 
 * @author yepanpan
 * @date 2024-04-11
 */
public interface IZaSysTypeService extends IService<ZaSysType>
{
    /**
     * 查询设备类型
     *
     * @param jbCode 青鸟设备类型代码
     * @return 设备类型
     */
    public ZaSysType selectZaSysTypeByJbCode(String jbCode);

    /**
     * 查询设备类型
     *
     * @param code 设备类型代码
     * @return 设备类型
     */
    public ZaSysType selectZaSysTypeByCode(String code);

    /**
     * 查询设备类型
     *
     * @param name 设备类型名称
     * @return 设备类型
     */
    public ZaSysType selectZaSysTypeByName(String name);

    /**
     * 查询设备类型
     *
     * @param id 设备类型主键
     * @return 设备类型
     */
    public ZaSysType selectZaSysTypeById(Long id);

    /**
     * 查询设备类型列表
     * 
     * @param zaSysType 设备类型
     * @return 设备类型集合
     */
    public List<ZaSysType> selectZaSysTypeList(ZaSysType zaSysType);

    /**
     * 新增设备类型
     * 
     * @param zaSysType 设备类型
     * @return 结果
     */
    public int insertZaSysType(ZaSysType zaSysType);

    /**
     * 修改设备类型
     * 
     * @param zaSysType 设备类型
     * @return 结果
     */
    public int updateZaSysType(ZaSysType zaSysType);

    /**
     * 批量删除设备类型
     * 
     * @param ids 需要删除的设备类型主键集合
     * @return 结果
     */
    public int deleteZaSysTypeByIds(Long[] ids);

    /**
     * 删除设备类型信息
     * 
     * @param id 设备类型主键
     * @return 结果
     */
    public int deleteZaSysTypeById(Long id);
}
