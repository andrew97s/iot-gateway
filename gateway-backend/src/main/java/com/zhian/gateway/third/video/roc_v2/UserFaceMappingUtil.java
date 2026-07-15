package com.zhian.gateway.third.video.roc_v2;

import cn.hutool.core.io.FileUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Set;

/**
 * 人脸相关工具类
 *
 * @author tongwenjin
 * @since 2025 -4-9
 */
public class UserFaceMappingUtil {

    /**
     * The constant PROPERTIES_FILE.
     */
    public static final String PROPERTIES_FILE = "props/user_mapping.properties";

    static {
        // 确保properties文件存在
        FileUtil.touch(PROPERTIES_FILE);
    }

    /**
     * 清空.
     */
    public static void clear() {
        Properties props = new Properties();

        try (FileInputStream in = new FileInputStream(PROPERTIES_FILE)) {
            props.load(in);

            props.clear();

            props.store(
                    Files.newOutputStream(Paths.get(PROPERTIES_FILE)),
                    "face module id mapping"
            );

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除指定用户数据
     *
     * @param id the id
     * @return the string
     */
    public static String delUser(Integer id) {
        Properties props = new Properties();

        try (FileInputStream in = new FileInputStream(PROPERTIES_FILE)) {
            props.load(in);

            String businessId = props.remove(id + "").toString();

            props.store(
                    Files.newOutputStream(Paths.get(PROPERTIES_FILE)),
                    "face module id mapping"
            );

            return businessId;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Set<Object> getAll() {
        Properties props = new Properties();

        try (FileInputStream in = new FileInputStream(PROPERTIES_FILE)) {
            props.load(in);
            return props.keySet();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 查询指定用户bizId.
     *
     * @param id the id
     * @return the string
     */
    public static String loadUser(Integer id) {
        Properties props = new Properties();

        try (FileInputStream in = new FileInputStream(PROPERTIES_FILE)) {
            props.load(in);
            return props.getProperty(id+"");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String loadByBizId(String bizId) {
        Properties props = new Properties();
        final String[] result = {null};

        try (FileInputStream in = new FileInputStream(PROPERTIES_FILE)) {
            props.load(in);
            props.forEach((key,value)->{
                if (bizId.equals(value)){
                    result[0] = key.toString();
                }
            });

            return result[0];
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 保存用户.
     *
     * @param id         the id
     * @param bizId the business id
     */
    public static void saveUser(Integer id , String bizId) {
        Properties props = new Properties();
        if (!FileUtil.exist(PROPERTIES_FILE)) {
            FileUtil.touch(PROPERTIES_FILE);
        }

        try (FileInputStream in = new FileInputStream(PROPERTIES_FILE)) {
            props.load(in);
            props.setProperty(id + "", bizId);

            props.store(
                    Files.newOutputStream(Paths.get(PROPERTIES_FILE)),
                    "face module id mapping"
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
