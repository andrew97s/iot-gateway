package com.zhian.gateway.third.jadebird.api;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysError;
import com.zhian.gateway.sys.service.IZaSysErrorService;
import com.zhian.gateway.sys.service.IZaSysMessageService;
import com.zhian.gateway.sys.utils.MessageUtil;
import com.zhian.gateway.third.jadebird.JadebirdBoxV2Handler;
import com.zhian.gateway.third.jadebird.vo.BoxMsg;
import com.zhian.gateway.third.jadebird.vo.BoxV2AlarmInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.OutputStream;


/**
 * 青鸟云盒-V2 http推送接口
 *
 * @author yepanpan
 */
@SuppressWarnings("CallToPrintStackTrace")
@RestController
@RequestMapping("jboxv2")
@Slf4j
public class JadeBirdBoxV2Api {
    /**
     * The Message service.
     */
    @Autowired
    private IZaSysMessageService messageService;
    /**
     * The Error service.
     */
    @Autowired
    protected IZaSysErrorService errorService;
    /**
     * The Handler.
     */
    @Autowired
    private JadebirdBoxV2Handler handler;

    /**
     * 云合告警
     *
     * @param alarmInfo the msg
     * @return the r
     */
    @RequestMapping("/alarm")
    public R<Object> alarmInfo(@RequestBody BoxV2AlarmInfo alarmInfo) {
        String alarmPicData = alarmInfo.getAlarmPicData();
        alarmInfo.setAlarmPicData(saveBase64(alarmPicData));
        alarmInfo.setSrcPicData("");
        alarmPicData = "";
        String alarmStr = JSON.toJSONString(alarmInfo);
        log.info("----接收到[青鸟云盒-V2]告警消息 BoxAlarmInfo: {}", alarmStr);

        BoxMsg boxMsg = new BoxMsg();
        boxMsg.setType(BoxMsg.TYPE_ALARM);
        boxMsg.setMsg(alarmStr);
        processMsg(boxMsg);
        return R.ok("OK");
    }

    /**
     * 云盒心跳
     *
     * @param msgStr the msg str
     * @return the ajax result
     */
    @RequestMapping("/heartbeat")
    public R<Object> heartbeat(@RequestBody String msgStr) {
        log.debug("box-heartbeat-v2 msg: {}", msgStr);

        BoxMsg boxMsg = new BoxMsg();
        boxMsg.setType(BoxMsg.TYPE_HEARTBEAT);
        boxMsg.setMsg(msgStr);
        processMsg(boxMsg);
        return R.ok("OK");
    }

    /**
     * 统一消息处理流程
     *
     * @param boxMsg the box msg
     */
    private void processMsg(BoxMsg boxMsg) {
        String results = "Y";
        int log = 0;
        try {
            handler.processMsg(boxMsg);
        } catch (Exception e) {
            e.printStackTrace();
            log = errorService.log(
                    ZaSysError.TYPE_MQ,
                    handler.getPlatform(),
                    "消息处理失败: " + e.getMessage(), boxMsg.getMsg()
            );
            results = "N";
        }

        //心跳数据不记录
        if (boxMsg.getType().equalsIgnoreCase(BoxMsg.TYPE_HEARTBEAT)) {
            MessageUtil.clear();
            return;
        }

        ZaSysDevice device = MessageUtil.getDevice();
        if (device != null) {
            // 此处截取消息内容前2000行
            messageService.log(MessageUtil.getDevice(), boxMsg.getType(), boxMsg.getMsg(), results);
        }
        else if (!boxMsg.getType().equalsIgnoreCase(BoxMsg.TYPE_HEARTBEAT) && log == 0) {
            errorService.log(ZaSysError.TYPE_MQ, handler.getPlatform(), "消息被忽略", boxMsg.getMsg());
        }
    }

    private String saveBase64(String base64String) {
        // 解码 Base64
        byte[] imageBytes = Base64.decode(base64String);

        // 指定输出文件路径
        String outputPath = "/home/zhian/app/fire/upload/box_alarm_pics/" + IdUtil.fastSimpleUUID() + ".png";

        // 保存为本地文件
        try (OutputStream outputStream = FileUtil.getOutputStream(new File(outputPath))) {
            IoUtil.write(outputStream, true, imageBytes);
            log.info("图片已成功保存至：" + outputPath);
        } catch (Exception e) {
            log.error("图片保存失败：" + e.getMessage());
        }

        return "file:" + outputPath;
    }


}
