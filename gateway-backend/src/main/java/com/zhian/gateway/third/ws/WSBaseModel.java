package com.zhian.gateway.third.ws;

import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.utils.uuid.SnowflakeIdWorker;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Transient;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 推送给前端WS消息的基础类
 */
@SuperBuilder
@NoArgsConstructor
public abstract class WSBaseModel {

    private Long id;
    private Long deptId;

    private String title;

    private String actionType;

    private String actionSets;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getActionSets() {
        return actionSets;
    }

    public void setActionSets(String actionSets) {
        this.actionSets = actionSets;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public static Map<Class<? extends WSBaseModel> , Field[]> fieldMap = new ConcurrentHashMap<>();

    /**
     * Gets message type.
     *
     * @return the message type
     */
    public abstract String getMessageType();

    /**
     * To json msg string.
     *
     * @return the string
     */
    public String toJsonMsg() {
        // 转之前前置处理
        if (!preSet()) {
            return null;
        }

        if (id == null) {
            id = SnowflakeIdWorker.getInstance().nextId();
        }

        JSONObject json = new JSONObject();

        // 一级属性
        json.put("id", id);
        json.put("title", title);
        json.put("actionType", actionType);
        json.put("actionSets", actionSets);
        json.put("deptId", deptId);
        json.put("type", getMessageType());

        // 二级属性
        JSONObject dataJson = new JSONObject();
        json.put("data", dataJson);

        Field[] fields ;

        // 优先取缓存
        if ((fields = fieldMap.get(this.getClass())) == null) {
            fieldMap.put(
                    this.getClass(),
                    this.getClass().getDeclaredFields()
            );
            fields = fieldMap.get(this.getClass());
        }

        for (Field f : fields) {
            if (f.getAnnotation(Transient.class) != null) {
                continue;
            }
            f.setAccessible(true);
            try {
                Object v = f.get(this);
                if (v != null) {
                    dataJson.put(f.getName(), v);
                }
            } catch (Exception e) {
                System.err.println("error:"+f.getName());
                e.printStackTrace();
            }
        }
        return json.toJSONString();
    }

    /**
     * 转json之前进行前置处理。
     *
     * @return true 继续处理  false 转的结果为null
     */
    public boolean preSet() {
        return true;
    }
}
