package com.zhian.gateway.third.hk.ezviz;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.zhian.gateway.common.constant.Constants;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.consts.DeviceTypeEnum;
import com.zhian.gateway.framework.disruptor.DisruptorUtil;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.third.common.BasePlatformHandler;
import com.zhian.gateway.third.common.bo.DeviceSyncInfo;
import com.zhian.gateway.third.common.bo.ProcessInfo;
import com.zhian.gateway.third.common.bo.SyncDevice;
import com.zhian.gateway.third.common.util.DeviceUtil;
import com.zhian.gateway.third.hk.ezviz.vo.*;
import com.zhian.gateway.third.hk.ezviz.vo.*;
import com.zhian.gateway.third.vo.ControlVo;
import com.zhian.gateway.third.vo.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.zhian.gateway.third.hk.ezviz.consts.EzvizConstants.*;

/**
 * 萤石云业务处理
 *
 * <br>
 * 对接文档见 <a href="https://open.ys7.com/help/1414">萤石云API</a>
 *
 * @author tongwenjin
 * @since 2024-11-26
 */
@Component
@Slf4j
public class EzvizHandler extends BasePlatformHandler<WebhookReq> {

    private Long tokenExpireTime;

    private String token;

    private ZaSysPlatform platform;

    @Override
    public boolean start(ZaSysPlatform platform) {
        this.platform = platform;

        // 校验配置是否合法
        try {
            Assert.notBlank(
                    platform.getConfigStr("appKey"),
                    "萤石云初始化失败,配置项:{appKey} 不能为空!"
            );
            Assert.notBlank(
                    platform.getConfigStr("appSecret"),
                    "萤石云初始化失败,配置项:{appSecret} 不能为空!"
            );
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        // 尝试全量同步设备信息
        DisruptorUtil.push(()->{
            fetchAllDevice(0);
        });

        running = true;

        return true;
    }

    @Override
    public DeviceSyncInfo syncDeviceStatus() {
        fetchAllDevice(0);
        return DeviceSyncInfo.success(1);
    }

    @Override
    public boolean stop() {
        running = false;
        return true;
    }

    @Override
    public String getPlatform() {
        return "ezviz";
    }

    @Override
    public String getProtocol() {
        return "ezviz";
    }

    @Override
    public R doControl(ControlVo controlVo) {
        // 获取流播放地址
        if (ControlVo.CMD_STREAM.equals(controlVo.getCommand())) {
            // 创建&执行拉流请求
            FetchDeviceStreamReq req = new FetchDeviceStreamReq();
            req.setAccessToken(fetchToken());
            req.setDeviceSerial(controlVo.getCode());
            String streamUrl = platform.getConfigStr(
                    "streamReqUrl", "https://open.ys7.com/api/lapp/v2/live/address/get"
            );
            String streamResp = HttpUtil.post(streamUrl, req.getRequestAsMap());

            // 处理响应
            FetchDeviceStreamResp resp = JSON.parseObject(streamResp, FetchDeviceStreamResp.class);
            if (resp.isSuccess() && StrUtil.isNotBlank(resp.getData().getUrl())) {
                // 此处从萤石云返回的流地址有效期默认为24h,过期需重新拉取
                return R.success(resp.getData().getUrl());
            }
            else {
                log.error("萤石云获取流失败,msg:{}" , resp.getMsg());
                R.error("获取流失败!");
            }
        } else {
            return R.error("萤石云暂未实现反控(code: " + controlVo.getCommand() + " )功能!");
        }

        return R.error("萤石云暂未实现反控功能!");
    }

    @Override
    public ProcessInfo doProcessMsg(WebhookReq req) {
        // 消息类型
        String type = req.getHeader().getType();
        ZaSysDevice device = null;
        String msgType = "alarm";
        MqMessage msg = null;
        switch (type) {
            // 心跳 - 直接忽略
            case MSG_TYPE_API:
                return null;
            // 设备状态 - TODO 待实现
            case MSG_TYPE_STATUS:
                DeviceStatusBody statusBody = req.getReqBody(DeviceStatusBody.class);
                return null;
            // 告警
            case MSG_TYPE_ALARM:
                AlarmBody alarmBody = req.getReqBody(AlarmBody.class);
                device = DeviceUtil.syncDevice(
                        SyncDevice.builder()
                                .code(alarmBody.getDevSerial())
                                .name(alarmBody.getChannelName())
                                .typeCode(DeviceTypeEnum.ICFD.getCode())
                                .pfCode(getPlatform())
                                .build()
                        , this
                );
                // 解析告警图片
                List<AlarmBody.Picture> picList = alarmBody.getPictureList();
                String imageUrl = null;
                if (CollUtil.isNotEmpty(picList)) {
                    imageUrl = picList.stream()
                            .map(AlarmBody.Picture::getUrl)
                            .collect(Collectors.joining(","));
                }
                // 推送告警消息
                msg = genAlarm(device, alarmBody.getAlarmType(), imageUrl, JSON.toJSONString(req));
                break;
            // 设备在离线
            case MSG_TYPE_ONLINE:
                DeviceOnlineBody onlineBody = req.getReqBody(DeviceOnlineBody.class);
                // 同步设备信息
                DeviceUtil.syncDevice(
                        SyncDevice.builder()
                                .code(onlineBody.getSubSerial())
                                .online(DEVICE_ONLINE.equals(onlineBody.getMsgType()) ? Constants.YES : Constants.NO)
                                .name(onlineBody.getDeviceName())
                                .model(onlineBody.getDevType())
                                .ip(onlineBody.getNatIp())
                                .pfCode(getPlatform())
                                .typeCode(DeviceTypeEnum.ICFD.getCode())
                                .wireless(Constants.YES)
                                .build(), this
                );
                break;
        }

        return ProcessInfo.newInstance(
                device , JSON.toJSONString(req) ,msgType , Collections.singletonList(msg)
        );
    }

    private void fetchAllDevice(Integer currentPage) {
        // 分页查询设备信息&同步
        FetchDeviceReq req = new FetchDeviceReq();
        req.setPageStart(currentPage);
        req.setAccessToken(fetchToken());

        // 发送设备查询请求
        String deviceUrl = platform.getConfigStr("deviceReqUrl", "https://open.ys7.com/api/lapp/device/list");
        String deviceResp = HttpUtil.post(deviceUrl, req.getRequestAsMap());
        FetchDeviceResp resp = JSON.parseObject(deviceResp, FetchDeviceResp.class);

        if (resp.isSuccess() && CollUtil.isNotEmpty(resp.getData())) {
            FetchDeviceResp.PageData pageData = resp.getPage();
            List<FetchDeviceResp.DeviceData> deviceData = resp.getData();
            log.info(" 查询萤石云设备第 {} 页,返回 {} 条设备数据!", currentPage, deviceData.size());
            // 依次遍历&同步设备信息
            for (FetchDeviceResp.DeviceData device : deviceData) {
                DeviceUtil.syncDevice(
                        SyncDevice.builder()
                                .code(device.getDeviceSerial())
                                .name(device.getDeviceName())
                                .ip(device.getNetAddress())
                                .pfCode(getPlatform())
                                .wireless(Constants.YES)
                                .typeCode(DeviceTypeEnum.ICFD.getCode())
                                .model(device.getDeviceType())
                                .online(device.getStatus() + "")
                                .remark(JSON.toJSONString(device))
                                .build() , this
                );
            }
            // 当前页不是尾页 - 递归执行
            if (pageData.getPage() * pageData.getSize() < pageData.getTotal() && currentPage <= 100) {
                fetchAllDevice(req.getPageStart() + 1);
            }
        } else if (!resp.isSuccess()) {
            log.error("查询萤石云设备列表(第 {} 页)失败,msg:{}", currentPage, resp.getMsg());
            return;
        }
        log.info(" 查询萤石云设备第 {} 页,返回数据为空!", currentPage);
    }


    private String fetchToken() {
        // token未过期 - 直接返回
        if (StrUtil.isNotBlank(token) && (tokenExpireTime - System.currentTimeMillis() > 10 * 60 * 1000L)) {
            return token;
        }
        // 获取配置
        String tokenReqUrl = platform.getConfigStr(
                "tokenReqUrl", "https://open.ys7.com/api/lapp/token/get"
        );
        String appKey = platform.getConfigStr("appKey");
        String appSecret = platform.getConfigStr("appSecret");
        // 发送token请求
        String tokenResp = HttpUtil.post(tokenReqUrl, new FetchTokenReq(appKey, appSecret).getRequestAsMap());
        FetchTokenResp resp = JSON.parseObject(tokenResp, FetchTokenResp.class);
        // 处理token响应
        if (resp.isSuccess() && StrUtil.isNotBlank(resp.getData().getAccessToken())) {
            this.token = resp.getData().getAccessToken();
            this.tokenExpireTime = resp.getData().getExpireTime();
        } else {
            log.error("获取萤石云token失败,response:{}", tokenResp);
            throw new RuntimeException("操作失败,获取萤石云token为空!");
        }

        return token;
    }
}
