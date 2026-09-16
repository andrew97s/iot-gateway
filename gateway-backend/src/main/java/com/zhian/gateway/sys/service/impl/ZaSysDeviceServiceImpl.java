package com.zhian.gateway.sys.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.zhian.gateway.common.core.cache.Cache;
import com.zhian.gateway.common.exception.ServiceException;
import com.zhian.gateway.common.utils.DateUtils;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.common.utils.bean.BeanValidators;
import com.zhian.gateway.common.utils.spring.SpringUtils;
import com.zhian.gateway.common.utils.uuid.SnowflakeIdWorker;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysType;
import com.zhian.gateway.sys.mapper.ZaSysDeviceMapper;
import com.zhian.gateway.sys.mapper.ZaSysTypeMapper;
import com.zhian.gateway.sys.service.IZaSysDeviceService;
import com.zhian.gateway.third.ThirdApplicationRunner;
import com.zhian.gateway.third.ThirdHandler;
import com.zhian.gateway.third.vo.MqMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.validation.Validator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 接入设备Service业务层处理
 *
 * @author yepanpan
 * @date 2024-05-13
 */
@Service
public class ZaSysDeviceServiceImpl extends ServiceImpl<ZaSysDeviceMapper, ZaSysDevice> implements IZaSysDeviceService {
    private static final Logger log = LoggerFactory.getLogger(ZaSysDeviceServiceImpl.class);
    private static final String CACHE_NAME = "za_sys_device";
    @Autowired
    private Cache cache;
    @Autowired
    protected Validator validator;
    @Autowired
    private ZaSysDeviceMapper zaSysDeviceMapper;
    @Autowired
    private ZaSysTypeMapper zaSysTypeMapper;

    @PostConstruct
    public void reCache() {
        cache.deleteObject(CACHE_NAME);
    }

    /**
     * 查询接入设备
     *
     * @param code 接入设备代码
     * @param net  网关
     * @return 接入设备
     */
    @Override
    public ZaSysDevice selectZaSysDeviceByCode(String code, String net) {
        // 设备编码不能为空
        if (StringUtils.isEmpty(code)) {
            return null;
        }

        // 缓存key  = 网关 + 设备编码
        String key = (net == null ? "" : net + "_") + code;
        // 缓存查询对应设备ID
        Long id = cache.getCacheMapValue(CACHE_NAME, key);
        if (id != null) {
            return selectZaSysDeviceById(id);
        }
        // 缓存未命中 ， 查询DB & 刷新缓存
        ZaSysDevice dc = zaSysDeviceMapper.selectZaSysDeviceByCode(code, net);
        if (dc != null) {
            cache.setCacheMapValue(CACHE_NAME, key, dc.getId());
        }
        return dc;
    }

    @Override
    public ZaSysDevice selectByCode(String code, String pfCode) {
        return zaSysDeviceMapper.selectByCode(code, pfCode);
    }

    /**
     * 查询接入设备
     *
     * @param id 接入设备主键
     * @return 接入设备
     */
    @Override
    public ZaSysDevice selectZaSysDeviceById(Long id) {
        // 从缓存查询设备信息
        String key = "id_" + id;
        ZaSysDevice zaSysDevice = cache.getCacheMapValue(CACHE_NAME, key);
        if (zaSysDevice != null) {
            return zaSysDevice;
        }
        // 缓存未命中 ， 查询DB & 刷新缓存
        zaSysDevice = zaSysDeviceMapper.selectZaSysDeviceById(id);
        if (zaSysDevice != null) {
            cache.setCacheMapValue(CACHE_NAME, key, zaSysDevice);
        }
        return zaSysDevice;
    }

    /**
     * 查询接入设备列表
     *
     * @param zaSysDevice 接入设备
     * @return 接入设备
     */
    @Override
    public List<ZaSysDevice> selectZaSysDeviceList(ZaSysDevice zaSysDevice) {
        return zaSysDeviceMapper.selectZaSysDeviceList(zaSysDevice);
    }

    /**
     * 新增接入设备
     *
     * @param zaSysDevice 接入设备
     * @return 结果
     */
    @Override
    public int insertZaSysDevice(ZaSysDevice zaSysDevice) {
        if (zaSysDevice.getRemark() != null) {
            if (zaSysDevice.getRemark().length() > 500) {
                zaSysDevice.setRemark(zaSysDevice.getRemark().substring(0, 498));
            }
        }
        zaSysDevice.setId(SnowflakeIdWorker.getInstance().nextId());
        zaSysDevice.setCreateTime(DateUtils.getNowDate());
        return zaSysDeviceMapper.insertZaSysDevice(zaSysDevice);
    }

    /**
     * 修改接入设备
     *
     * @param zaSysDevice 接入设备
     * @return 结果
     */
    @Override
    public int updateZaSysDevice(ZaSysDevice zaSysDevice) {
        cache.deleteCacheMapValue(CACHE_NAME, "id_" + zaSysDevice.getId());
        if (StringUtils.isEmpty(zaSysDevice.getNet())) {
            cache.deleteCacheMapValue(CACHE_NAME, zaSysDevice.getCode());
        } else {
            cache.deleteCacheMapValue(CACHE_NAME, zaSysDevice.getNet() + "_" + zaSysDevice.getCode());
        }
        zaSysDevice.setUpdateTime(DateUtils.getNowDate());
        if (zaSysDevice.getRemark() != null) {
            if (zaSysDevice.getRemark().length() > 500) {
                zaSysDevice.setRemark(zaSysDevice.getRemark().substring(498));
            }
        }
        return zaSysDeviceMapper.updateZaSysDevice(zaSysDevice);
    }

    @Override
    public int updCommTime(Long deviceId) {
        return getBaseMapper()
                .update(
                        Wrappers.lambdaUpdate(ZaSysDevice.class)
                                .set(ZaSysDevice::getLastCommTime, new Date())
                                .eq(ZaSysDevice::getId, deviceId)
                );
    }

    /**
     * 批量删除接入设备
     *
     * @param ids 需要删除的接入设备主键
     * @return 结果
     */
    @Override
    public int deleteZaSysDeviceByIds(Long[] ids) {
        cache.deleteObject(CACHE_NAME);
        return zaSysDeviceMapper.deleteZaSysDeviceByIds(ids);
    }

    /**
     * 删除接入设备信息
     *
     * @param id 接入设备主键
     * @return 结果
     */
    @Override
    public int deleteZaSysDeviceById(Long id) {
        cache.deleteCacheMapValue(CACHE_NAME, "id_" + id);
        return zaSysDeviceMapper.deleteZaSysDeviceById(id);
    }

    /**
     * 重新推送所有的设备数据，使对接应用可能与本模块数据保持一致
     * 1.后台进行执行
     * 2.每次10条数据，减少应用端压力
     * 3.间隔三秒钟
     */
    @Override
    public void pushDevice() {
        new Thread(() -> {
            int page = 1;
            MqMessage mqMessage = new MqMessage();
            mqMessage.setEvent(MqMessage.EVENT_DEVICE);
            mqMessage.setTime(new Date());

            MqMessage.Facility facility = new MqMessage.Facility();

            ThirdHandler gatewayHandler = SpringUtils.getBean("messageSyncHandler");
            while (true) {
                PageHelper.startPage(page, 10);
                List<ZaSysDevice> list = zaSysDeviceMapper.selectZaSysDeviceList(new ZaSysDevice());
                for (ZaSysDevice device : list) {

                    facility.setWireless(device.getWireless().equals("Y"));
                    facility.setType(device.getType());
                    facility.setCode(device.getCode());
                    facility.setName(device.getName());
                    facility.setNet(device.getNet());

                    ThirdHandler thirdHandler = ThirdApplicationRunner.getHandler(device.getPfCode());
                    mqMessage.setDeviceId(device.getId());
                    mqMessage.setFacility(facility);
                    mqMessage.setMsgData(null);
                    mqMessage.setProtocol(thirdHandler.getProtocol());
                    mqMessage.setUuid(SnowflakeIdWorker.getInstance().nextStringId());
                    gatewayHandler.processMsg(mqMessage);
                }

                if (list == null || list.size() < 100) {
                    break;
                }
                page++;
                try {

                    Thread.sleep(1000L * 3);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void purge() {
        zaSysDeviceMapper.purge();
        cache.deleteObject(CACHE_NAME);
    }

    @Override
    public void updateDeviceOnline(String code, String pfCode, boolean online) {
        ZaSysDevice device = zaSysDeviceMapper.selectByCode(code, pfCode);
        if (device == null) return;

        String onlineVal = online ? "1" : "0";
        // 状态未变化时跳过
        if (onlineVal.equals(device.getOnline())) return;

        device.setOnline(onlineVal);
        device.setUpdateTime(DateUtils.getNowDate());
        zaSysDeviceMapper.updateZaSysDevice(device);
        // 使缓存失效
        cache.deleteCacheMapValue(CACHE_NAME, "id_" + device.getId());
        String cacheKey = (device.getNet() == null ? "" : device.getNet() + "_") + code;
        cache.deleteCacheMapValue(CACHE_NAME, cacheKey);

        // 通过 MessageSyncHandler 推送设备状态变更事件
        try {
            MqMessage mqMessage = new MqMessage();
            mqMessage.setEvent(MqMessage.EVENT_DEVICE);
            mqMessage.setTime(new java.util.Date());
            mqMessage.setDeviceId(device.getId());

            MqMessage.Facility facility = new MqMessage.Facility();
            facility.setCode(device.getCode());
            facility.setName(device.getName());
            facility.setType(device.getType());
            facility.setNet(device.getNet());
            facility.setWireless("Y".equals(device.getWireless()));
            mqMessage.setFacility(facility);

            // 把在线状态放到 msgData
            Map<String, Object> data = new HashMap<>();
            data.put("online", online);
//            mqMessage.setMsgData(data);
            mqMessage.setUuid(SnowflakeIdWorker.getInstance().nextStringId());

            ThirdHandler gatewayHandler = SpringUtils.getBean("messageSyncHandler");
            gatewayHandler.processMsg(mqMessage);
        } catch (Exception e) {
            log.error("推送设备在线状态变更事件失败: code={}, online={}", code, online, e);
        }
    }

    @Override
    public List<Map<String, Object>> selectOnlineStats() {
        return zaSysDeviceMapper.selectOnlineStats();
    }


    /**
     * 导入数据
     *
     * @param dataList        数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importData(List<ZaSysDevice> dataList, Boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(dataList) || dataList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }
        int successNum = 0;
        int failureNum = 0;
        //分类
        List<ZaSysType> typeList = zaSysTypeMapper.selectZaSysTypeList(new ZaSysType());
        Map<String, String> typeMap = new HashMap<>();
        typeList.forEach(t -> {
            if (!typeMap.containsKey(t.getName())) {
                typeMap.put(t.getName(), t.getCode());
            }
        });
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (ZaSysDevice zaSysDevice : dataList) {
            try {
                //验证分类
                if (StringUtils.isEmpty(zaSysDevice.getType())) {
                    throw new ServiceException("设备分类不能为空!");
                }
                if (typeMap.containsKey(zaSysDevice.getType())) {
                    zaSysDevice.setType(typeMap.get(zaSysDevice.getType()));
                }

                // 验证是否旧设备
                ZaSysDevice old = selectZaSysDeviceByCode(zaSysDevice.getCode(), zaSysDevice.getNet());
                if (StringUtils.isNull(old)) {
                    BeanValidators.validateWithException(validator, zaSysDevice);
                    zaSysDevice.setCreateBy(operName);
                    insertZaSysDevice(zaSysDevice);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、设备 " + zaSysDevice.getCode() + " 导入成功");
                } else if (isUpdateSupport) {
                    BeanValidators.validateWithException(validator, zaSysDevice);
                    zaSysDevice.setUpdateBy(operName);
                    zaSysDevice.setId(old.getId());
                    updateZaSysDevice(zaSysDevice);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、设备 " + zaSysDevice.getCode() + " 更新成功");
                } else {
                    failureNum++;
                    failureMsg.append("<br/>" + failureNum + "、设备 " + zaSysDevice.getName() + "(" + zaSysDevice.getCode() + ")" + " 已存在");
                }
            } catch (Exception e) {
                failureNum++;
                String msg = "<br/>" + failureNum + "、设备 " + zaSysDevice.getName() + " 导入失败：";
                failureMsg.append(msg + e.getMessage());
                log.error(msg, e);
            }
        }
        if (failureNum > 0) {
            failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确，错误如下：");
            throw new ServiceException(failureMsg.toString());
        } else {
            successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条，数据如下：");
        }
        return successMsg.toString();
    }
}
