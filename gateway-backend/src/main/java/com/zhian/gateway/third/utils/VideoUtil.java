package com.zhian.gateway.third.utils;

import com.zhian.gateway.common.config.ZhianConfig;
import com.zhian.gateway.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.UUID;

/**
 * 视频处理工具
 */
@Slf4j
public class VideoUtil {

    /**
     * 判断当前操作系统是不是linux
     */
    public static boolean isLinux(){
        return System.getProperty("os.name").toLowerCase().contains("linux");
    }

    /**
     * 判断FFMPEG路径
     * @return
     */
    public static String ffmpegHome(){
        String ffmpeg = "ffmpeg" + (isLinux()?"":".exe");
        String home = System.getProperty("user.dir");
        File ffmpegFile = new File(home+"/"+ffmpeg);
        if(ffmpegFile.exists()){
            return ffmpegFile.getAbsolutePath();
        }
        home = System.getProperty("user.home");
        ffmpegFile = new File(home+"/"+ffmpeg);
        if(ffmpegFile.exists()){
            return ffmpegFile.getAbsolutePath();
        }
        return ffmpeg;
    }

    /**
     * 通过ffmpeg自己截图
     * @param rtsp
     * @return
     */
    public static String ffmpeg(String rtsp){
        String ffmpeg = ffmpegHome();
        if(StringUtils.isEmpty(ffmpeg)){
            log.error("未找到ffmpeg主程序: {}", ffmpeg);
            return null;
        }

        // 存放目标图片的完整路径，如：/home/dzkj/data/firemonitor/2023-4aa0-fbb1f5f46192.jpg
        File f = new File(ZhianConfig.getUploadPath());
        if(!f.exists()){
            f.mkdirs();
        }
        String path  = "/" + UUID.randomUUID().toString()+ ".jpg";
        String savePath = ZhianConfig.getUploadPath() + path;

        String cmd = ffmpeg + " -i " + rtsp + " -frames:v 1 -timeout 3 -f image2 " + savePath;
        try {
            log.info("exec: {}", cmd);
            Process process = Runtime.getRuntime().exec(cmd);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //处理输出流，由于process机制原因会导致死锁，所以需要在waitfor方法之前，用于处理inputstream中缓冲区的数据
                        InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream());
                        BufferedReader reader = new BufferedReader(inputStreamReader);
                        String line;
                        while ((line = reader.readLine()) != null) {
                            log.debug("ffmpeg output: {} ", line);
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }).start();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //处理输出流，由于process机制原因会导致死锁，所以需要在waitfor方法之前，用于处理inputstream中缓冲区的数据
                        InputStreamReader inputStreamReader = new InputStreamReader(process.getErrorStream());
                        BufferedReader reader = new BufferedReader(inputStreamReader);
                        String line;
                        while ((line = reader.readLine()) != null) {
                            log.debug("ffmpeg error: {} ", line);
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }).start();
            //返回0则表示输出正常,目前只关注是否生成文件
            int resultCode = process.waitFor();
            log.info("ffmpeg exec exit {}", resultCode);
            f = new File(savePath);
            if(!f.exists()){
                log.warn("ffmpeg exec get no file for {}", rtsp);
                return null;
            }else if(f.length() == 0){
                log.warn("ffmpeg exec get empty file for {}", rtsp);
                f.delete();
                return null;
            }else{
                return path;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
