package com.zhian.gateway.sys.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhian.gateway.common.annotation.DataScope;
import com.zhian.gateway.common.core.cache.Cache;
import com.zhian.gateway.common.exception.ServiceException;
import com.zhian.gateway.common.utils.DateUtils;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.common.utils.uuid.SnowflakeIdWorker;
import com.zhian.gateway.sys.domain.ZaSysDataset;
import com.zhian.gateway.sys.mapper.ZaSysDatasetMapper;
import com.zhian.gateway.sys.service.IZaSysDatasetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * 数据集Service业务层处理
 * 
 * @author yepanpan
 * @date 2024-04-11
 */
@Service
@Slf4j
public class ZaSysDatasetServiceImpl extends ServiceImpl<ZaSysDatasetMapper , ZaSysDataset> implements IZaSysDatasetService
{
    private static  final  String CACHE_MAP = "za_sys_dataset";
    @Autowired
    private Cache cache;

    @Autowired
    private ZaSysDatasetMapper zaSysDatasetMapper;

    /**
     * 缓存
     * @return
     */
    @PostConstruct
    public int reCache(){
        cache.deleteObject(CACHE_MAP);
        List<ZaSysDataset> list = zaSysDatasetMapper.selectZaSysDatasetList(new ZaSysDataset());
        list.forEach(t->{
            cache.setCacheMapValue(CACHE_MAP, "id_"+t.getId(), t);
            cache.setCacheMapValue(CACHE_MAP, "code_"+t.getCode(), t.getId());
        });
        return list.size();
    }

    /**
     * 查询数据集
     *
     * @param code 数据集代码
     * @return 数据集
     */
    public ZaSysDataset selectZaSysDatasetByCode(String code){
        Long id = cache.getCacheMapValue(CACHE_MAP, "code_"+code);
        if(id != null){
            return selectZaSysDatasetById(id);
        }else{
            return zaSysDatasetMapper.selectZaSysDatasetByCode(code);
        }
    }
    /**
     * 查询数据集
     * 
     * @param id 数据集主键
     * @return 数据集
     */
    @Override
    public ZaSysDataset selectZaSysDatasetById(Long id)
    {
        ZaSysDataset zaSysDataset = cache.getCacheMapValue(CACHE_MAP, "id_"+id);
        if(zaSysDataset != null){
            return zaSysDataset;
        }
        return zaSysDatasetMapper.selectZaSysDatasetById(id);
    }

    /**
     * 查询数据集列表
     * 
     * @param zaSysDataset 数据集
     * @return 数据集
     */
    @Override
    public List<ZaSysDataset> selectZaSysDatasetList(ZaSysDataset zaSysDataset)
    {
        return zaSysDatasetMapper.selectZaSysDatasetList(zaSysDataset);
    }

    /**
     * 新增数据集
     * 
     * @param zaSysDataset 数据集
     * @return 结果
     */
    @Override
    public int insertZaSysDataset(ZaSysDataset zaSysDataset)
    {
        if(!checkSql(zaSysDataset.getSqls())){
            throw new ServiceException("查询SQL不合规则");
        }

        ZaSysDataset old = selectZaSysDatasetByCode(zaSysDataset.getCode());
        if(old != null){
            throw  new ServiceException("数据集代码不能重复");
        }
        zaSysDataset.setId(SnowflakeIdWorker.getInstance().nextId());
        zaSysDataset.setCreateTime(DateUtils.getNowDate());
        return zaSysDatasetMapper.insertZaSysDataset(zaSysDataset) + reCache();
    }

    /**
     * 修改数据集
     * 
     * @param zaSysDataset 数据集
     * @return 结果
     */
    @Override
    public int updateZaSysDataset(ZaSysDataset zaSysDataset)
    {
        if(!checkSql(zaSysDataset.getSqls())){
            throw new ServiceException("查询SQL不合规则");
        }

        zaSysDataset.setUpdateTime(DateUtils.getNowDate());
        return zaSysDatasetMapper.updateZaSysDataset(zaSysDataset) + reCache();
    }

    /**
     * 验证SQL是否合法
     * @param sql
     * @return
     */
    private boolean checkSql(String sql){
        sql = sql.toLowerCase();
        if(sql.contains("insert ") || sql.contains("delete") || sql.contains("update") || sql.contains("raplce")){
            return false;
        }
        return true;
    }

    /**
     * 批量删除数据集
     * 
     * @param ids 需要删除的数据集主键
     * @return 结果
     */
    @Override
    public int deleteZaSysDatasetByIds(Long[] ids)
    {
        return zaSysDatasetMapper.deleteZaSysDatasetByIds(ids) + reCache();
    }

    /**
     * 删除数据集信息
     * 
     * @param id 数据集主键
     * @return 结果
     */
    @Override
    public int deleteZaSysDatasetById(Long id)
    {
        return zaSysDatasetMapper.deleteZaSysDatasetById(id) + reCache();
    }

    /**
     * 执行数据集查询
     * 1.如果配置了参数，但是没有传递参数，会将参数替换成0
     * 2.支持在SQL里配置数据权限${dataScope}
     *
     * @param zaSysDataset
     * @return
     */
    @DataScope(deptAlias = "d", userAlias = "r")
    public Object query(ZaSysDataset zaSysDataset){
        ZaSysDataset ds = selectZaSysDatasetByCode(zaSysDataset.getCode());
        if(ds == null){
            return null;
        }

        String sql = ds.getSqls();

        //权限范围
        sql = sql.replaceAll("\\$\\{dataScope\\}", zaSysDataset.getParams().get("dataScope").toString());

        //处理参数，只支持绝对相等
        if(StringUtils.isNotEmpty(ds.getParam())){
            String params[] = ds.getParam().split(",");
            for(String p:params){
                if(zaSysDataset.getParams().containsKey(p)) {
                    sql = sql.replaceAll("\\$\\{" + p + "\\}", "'"+zaSysDataset.getParams().get(p).toString()+"'");
                }else{
                    sql = sql.replaceAll("\\$\\{" + p + "\\}", "0");
                }
                /*
                String[] pa = p.split("="); deptId=dept_id,前边是参数名，后边是字段名，没有字段名就自动驼峰转下划线
                String pn = pa.length == 0 ? StringUtils.toUnderScoreCase(pa[0]) : pa[1];
                if(zaSysDataset.getParams().containsKey(pa[0])) {
                    sql = sql.replaceAll("\\$\\{" + pa[0] + "\\}", pn + " = '"+zaSysDataset.getParams().get(p).toString()+"'");
                }else{
                    sql = sql.replaceAll("\\$\\{" + pa[0] + "\\}", "1");
                }
                */
            }
        }
        log.debug("ds query: {}", sql);
        List<Map> list = zaSysDatasetMapper.querySql(sql);
        //如果是计算查询，返回第一个结果，没有结果返回0
        if(ZaSysDataset.TYPE_COUNT.equalsIgnoreCase(ds.getType())){
            if(list == null || list.isEmpty()){
                return  0;
            }else{
                Map data = list.get(0);
                return data.get(data.keySet().iterator().next());
            }
        }else{
            return list;
        }
    }
}
