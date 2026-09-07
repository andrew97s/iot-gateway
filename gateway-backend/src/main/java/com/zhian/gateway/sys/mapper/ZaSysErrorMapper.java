package com.zhian.gateway.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhian.gateway.sys.domain.ZaSysError;
import org.apache.ibatis.annotations.Mapper;

import java.util.Date;
import java.util.List;

/**
 * 错误日志Mapper接口
 * 
 * @author yepanpan
 * @date 2024-04-11
 */
@Mapper
public interface ZaSysErrorMapper extends BaseMapper<ZaSysError>
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
     * 删除错误日志
     * 
     * @param id 错误日志主键
     * @return 结果
     */
    public int deleteZaSysErrorById(Long id);

    /**
     * 批量删除错误日志
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteZaSysErrorByIds(Long[] ids);

    /**
     * 清除错误日志
     * @param logTime
     * @return
     */
    public int cleanZaSysError(Date logTime);

    /**
     * 清除数据
     */
    void purge();

    /**
     * 查询指定平台的最近日志
     *
     * @param pfCode 平台代码
     * @param limit  返回条数
     */
    List<ZaSysError> selectByPlatform(@org.apache.ibatis.annotations.Param("pfCode") String pfCode,
                                      @org.apache.ibatis.annotations.Param("limit") int limit);
}
