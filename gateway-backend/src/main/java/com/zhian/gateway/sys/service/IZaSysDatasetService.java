package com.zhian.gateway.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhian.gateway.sys.domain.ZaSysDataset;

import java.util.List;

/**
 * 数据集Service接口
 * 
 * @author yepanpan
 * @date 2024-04-11
 */
public interface IZaSysDatasetService extends IService<ZaSysDataset>
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
     * 批量删除数据集
     * 
     * @param ids 需要删除的数据集主键集合
     * @return 结果
     */
    public int deleteZaSysDatasetByIds(Long[] ids);

    /**
     * 删除数据集信息
     * 
     * @param id 数据集主键
     * @return 结果
     */
    public int deleteZaSysDatasetById(Long id);

    /**
     * 执行数据集查询
     * @param zaSysDataset
     * @return
     */
    public Object query(ZaSysDataset zaSysDataset);
}
