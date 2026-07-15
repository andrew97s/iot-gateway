package com.zhian.gateway.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 接入设备Mapper接口
 *
 * @author yepanpan
 * @date 2024 -05-13
 */
public interface ZaSysDeviceMapper extends BaseMapper<ZaSysDevice>
{

    /**
     * 查询接入设备
     *
     * @param code 接入设备代码
     * @param net  接入设备网关
     * @return 接入设备
     */
    public ZaSysDevice selectZaSysDeviceByCode(@Param("code") String code, @Param("net") String net);

    /**
     * Select by code za sys device.
     *
     * @param code   the code
     * @param pfCode the pf code
     * @return the za sys device
     */
    ZaSysDevice selectByCode(@Param("code") String code, @Param("pfCode") String pfCode);

    /**
     * 查询接入设备
     *
     * @param id 接入设备主键
     * @return 接入设备
     */
    public ZaSysDevice selectZaSysDeviceById(Long id);

    /**
     * 查询接入设备列表
     *
     * @param zaSysDevice 接入设备
     * @return 接入设备集合
     */
    public List<ZaSysDevice> selectZaSysDeviceList(ZaSysDevice zaSysDevice);

    /**
     * 新增接入设备
     *
     * @param zaSysDevice 接入设备
     * @return 结果
     */
    public int insertZaSysDevice(ZaSysDevice zaSysDevice);

    /**
     * 修改接入设备
     *
     * @param zaSysDevice 接入设备
     * @return 结果
     */
    public int updateZaSysDevice(ZaSysDevice zaSysDevice);

    /**
     * 删除接入设备
     *
     * @param id 接入设备主键
     * @return 结果
     */
    public int deleteZaSysDeviceById(Long id);

    /**
     * 批量删除接入设备
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteZaSysDeviceByIds(Long[] ids);

    /**
     * 清除表数据
     */
    void purge();

    /**
     * 按平台统计设备数量（total/onlineCount）
     */
    List<java.util.Map<String, Object>> selectOnlineStats();
}
