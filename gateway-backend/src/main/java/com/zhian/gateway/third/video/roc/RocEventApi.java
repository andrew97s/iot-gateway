package com.zhian.gateway.third.video.roc;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson2.JSON;
import com.zhian.gateway.third.video.roc.common.RocConstants;
import com.zhian.gateway.third.video.roc.event.RocEvent;
import com.zhian.gateway.third.video.roc.event.RocFaceEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * ROC摄像头server
 *
 * @author tongwenjin
 * @since 2025-2-18
 */
@Slf4j
@RequestMapping
@RestController
@SuppressWarnings("CallToPrintStackTrace")
public class RocEventApi {

    @Autowired
    private RocHandler rocHandler;

    @RequestMapping("/DeviceEndianHeartbeat")
    public Object heartBeat(@RequestBody(required = false) String body) {
        log.info("heartBeat from ROC : {}", body);

        HashMap<String, Object> resp = new HashMap<>();

        resp.put("ReturnCode", 0);
        resp.put("ReturnStr", "OK");

        return resp;
    }

    @RequestMapping("/DeviceEndianEvent")
    public Object event(
            @RequestBody(required = false) String body,
            HttpServletRequest request,
            @RequestPart(required = false, name = "EventInfo") List<MultipartFile> events,
            @RequestPart(required = false, name = "file") List<MultipartFile> files
    ) throws IOException {
        if (CollUtil.isNotEmpty(events) && CollUtil.isNotEmpty(files)) {
            MultipartFile eventFile = events.get(0);
            MultipartFile eventPic = files.get(0);

            // 使用Hutool读取文件内容为字符串
            String eventJson = new String(eventFile.getBytes(), StandardCharsets.UTF_8);
            log.info("ROC event content :{}", eventJson);

            RocEvent event = JSON.parseObject(eventJson, RocEvent.class);

            // 人脸库内的人脸事件
            if (RocConstants.EVENT_FACE_COMPARE.equals(event.getEventType())) {
                RocFaceEvent faceEvent = new RocFaceEvent();

                // 组装VO
                RocEvent.Snap snap = event.getFaceCompareInfo().getSnap().get(0);
                faceEvent.setName(snap.getMatch().getName());
                faceEvent.setTime(new Date());
                faceEvent.setSimilarity(snap.getMatch().getSimilarity());
                faceEvent.setAction(event.getEventType());
                for(MultipartFile f:files){
                    if(f.getOriginalFilename().equalsIgnoreCase(snap.getMatch().getRegPic())){
                        faceEvent.setFaceImg(Base64.getEncoder().encodeToString(f.getBytes()));
                    }
                }
                rocHandler.processMsg(faceEvent);
            }else{
                log.debug("忽略ROC event content :{}", event.getEventType());
            }
        }

        HashMap<String, Object> resp = new HashMap<>();
        resp.put("ReturnCode", 0);
        resp.put("ReturnStr", "OK");

        return resp;
    }
}
