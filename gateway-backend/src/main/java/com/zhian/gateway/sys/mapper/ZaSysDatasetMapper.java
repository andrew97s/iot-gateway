package com.zhian.gateway.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhian.gateway.sys.domain.ZaSysDataset;

import java.util.List;
import java.util.Map;

/**
 * 数据集Mapper接口
 * 
 * @author yepanpan
 * @date 2024-04-11
 */
public interface ZaSysDatasetMapper extends BaseMapper<ZaSysDataset>
{
    /**
     * 查询数据集
     *
     * @param code 数据集代码
     * @return 数据集
     */
    public ZaSysDataset selectZaSysDatasetByCode(String code);

    /**
     * 查询数据集
     * 
     * @param id 数据集主键
     * @return 数据集
     */
    public ZaSysDataset selectZaSysDatasetById(Long id);

    /**
     * 查询数据集列表
     * 
     * @param zaSysDataset 数据集
     * @return 数据集集合
     */
    public List<ZaSysDataset> selectZaSysDatasetList(ZaSysDataset zaSysDataset);

    /**
     * 新增数据集
     * 
     * @param zaSysDataset 数据集
     * @return 结果
     */
    public int insertZaSysDataset(ZaSysDataset zaSysDataset);

    /**
     * 修改数据集
     * 
     * @param zaSysDataset 数据集
     * @return 结果
     */
    public int updateZaSysDataset(ZaSysDataset zaSysDataset);

    /**
     * 删除数据集
     * 
     * @param id 数据集主键
     * @return 结果
     */
    public int deleteZaSysDatasetById(Long id);

    /**
     * 批量删除数据集
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteZaSysDatasetByIds(Long[] ids);

    /**
     * 执行一次查询
     *
     * @param sql SQL语句
     * @return 结果
     */
    public List<Map> querySql(String sql);

}
