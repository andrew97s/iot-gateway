package com.zhian.gateway.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhian.gateway.sys.domain.ZaSysCascade;

import java.util.List;

/**
 * 级联平台Mapper接口
 * 
 * @author yepanpan
 * @date 2024-05-13
 */
public interface ZaSysCascadeMapper extends BaseMapper<ZaSysCascade>
{

    /**
     * 查询级联平台
     *
     * @param code 级联平台代码
     * @return 级联平台
     */
    public ZaSysCascade selectZaSysCascadeByCode(String code);

    /**
     * 查询级联平台
     * 
     * @param id 级联平台主键
     * @return 级联平台
     */
    public ZaSysCascade selectZaSysCascadeById(Long id);

    /**
     * 查询级联平台列表
     * 
     * @param zaSysCascade 级联平台
     * @return 级联平台集合
     */
    public List<ZaSysCascade> selectZaSysCascadeList(ZaSysCascade zaSysCascade);

    /**
     * 新增级联平台
     * 
     * @param zaSysCascade 级联平台
     * @return 结果
     */
    public int insertZaSysCascade(ZaSysCascade zaSysCascade);

    /**
     * 修改级联平台
     * 
     * @param zaSysCascade 级联平台
     * @return 结果
     */
    public int updateZaSysCascade(ZaSysCascade zaSysCascade);

    /**
     * 删除级联平台
     * 
     * @param id 级联平台主键
     * @return 结果
     */
    public int deleteZaSysCascadeById(Long id);

    /**
     * 批量删除级联平台
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteZaSysCascadeByIds(Long[] ids);
}
