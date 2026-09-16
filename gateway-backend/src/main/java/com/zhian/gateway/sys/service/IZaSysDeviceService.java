package com.zhian.gateway.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhian.gateway.sys.domain.ZaSysDevice;

import java.util.List;
import java.util.Map;

/**
 * 接入设备Service接口
 *
 * @author yepanpan
 * @date 2024 -05-13
 */
public interface IZaSysDeviceService extends IService<ZaSysDevice>
{

    /**
     * 查询接入设备
     *
     * @param code 接入设备代码
     * @param net  网关
     * @return 接入设备
     */
    public ZaSysDevice selectZaSysDeviceByCode(String code, String net);

    /**
     * Select by code za sys device.
     *
     * @param code   the code
     * @param pfCode the pf code
     * @return the za sys device
     */
    ZaSysDevice selectByCode(String code, String pfCode);

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
     * Upd comm time int.
     *
     * @param deviceId the device id
     * @return the int
     */
    int updCommTime(Long deviceId);

    /**
     * 批量删除接入设备
     *
     * @param ids 需要删除的接入设备主键集合
     * @return 结果
     */
    public int deleteZaSysDeviceByIds(Long[] ids);

    /**
     * 删除接入设备信息
     *
     * @param id 接入设备主键
     * @return 结果
     */
    public int deleteZaSysDeviceById(Long id);

    /**
     * 重新推送所有的设备数据，使对接应用可能与本模块数据保持一致
     */
    public void pushDevice();

    /**
     * Purge.
     */
    void purge();

    /**
     * 导入数据
     *
     * @param dataList        数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    public String importData(List<ZaSysDevice> dataList, Boolean isUpdateSupport, String operName);

    /**
     * 更新设备在线状态，并通过 MessageSyncHandler 推送设备状态变更事件
     *
     * @param code   设备编码
     * @param pfCode 平台代码
     * @param online 是否在线
     */
    void updateDeviceOnline(String code, String pfCode, boolean online);

    /**
     * 按平台统计设备在线/总数
     *
     * @return list of {pfCode, total, onlineCount}
     */
    List<Map<String, Object>> selectOnlineStats();
}
