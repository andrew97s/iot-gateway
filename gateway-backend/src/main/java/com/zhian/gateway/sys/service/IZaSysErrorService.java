package com.zhian.gateway.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhian.gateway.sys.domain.ZaSysError;

import java.util.List;

/**
 * 错误日志Service接口
 * 
 * @author yepanpan
 * @date 2024-04-11
 */
public interface IZaSysErrorService extends IService<ZaSysError>
{
    /**
     * 查询错误日志
     * 
     * @param id 错误日志主键
     * @return 错误日志
     */
    public ZaSysError selectZaSysErrorById(Long id);

    /**
     * 查询错误日志列表
     * 
     * @param zaSysError 错误日志
     * @return 错误日志集合
     */
    public List<ZaSysError> selectZaSysErrorList(ZaSysError zaSysError);

    /**
     * 新增错误日志
     * 
     * @param zaSysError 错误日志
     * @return 结果
     */
    public int insertZaSysError(ZaSysError zaSysError);

    /**
     * 修改错误日志
     * 
     * @param zaSysError 错误日志
     * @return 结果
     */
    public int updateZaSysError(ZaSysError zaSysError);

    /**
     * 批量删除错误日志
     * 
     * @param ids 需要删除的错误日志主键集合
     * @return 结果
     */
    public int deleteZaSysErrorByIds(Long[] ids);

    /**
     * 删除错误日志信息
     * 
     * @param id 错误日志主键
     * @return 结果
     */
    public int deleteZaSysErrorById(Long id);


    /**
     * 快速记录错误日志
     * @param type 类别
     * @param title 标题
     * @param error 错误
     * @param content 数据内容
     */
    public int log(String type, String title, String error, String content);

    /**
     * 快速记录关联平台的错误/事件日志
     * @param pfCode 平台代码
     * @param type 类别
     * @param title 标题
     * @param error 错误
     * @param content 数据内容
     */
    public int logWithPlatform(String pfCode, String type, String title, String error, String content);

    /**
     * 查询指定平台的最近日志（错误+事件）
     * @param pfCode 平台代码
     * @param limit 最多返回条数
     */
    public List<ZaSysError> selectByPlatform(String pfCode, int limit);

    /**
     * Purge.
     */
    void purge();
}
