package com.zhian.gateway.third;

import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.third.vo.ControlVo;

import java.util.Collections;
import java.util.List;

/**
 * 第三方接口启停控制器
 */
public interface ThirdHandler {
    /**
     * 启动对接
     *
     * @param platform the za sys platform
     * @return the boolean
     */
    boolean start(ZaSysPlatform platform);

    /**
     * 停止对接
     *
     * @return the boolean
     */
    boolean stop();

    /**
     * 确认第三方对接服务是否正常
     *
     * @return boolean
     */
    boolean isAlive();

    /**
     * 插件对接的平台代码
     *
     * @return platform code
     */
    String getPlatform();

    /**
     * 插件对接的平台协议
     *
     * @return protocol protocol
     */
    String getProtocol();

    /**
     * 反向控制
     *
     * @param controlVo the control vo
     * @return ajax result
     */
    R control(ControlVo controlVo);

    /**
     * 处理接收到的消息
     *
     * @param msgObj the msg obj
     */
    void processMsg(Object msgObj);

    // ========== 元数据方法（default，子类可选覆盖） ==========

    /**
     * 插件的协议描述，例如："海康威视 ISAPI 协议"
     */
    default String getDescription() {
        return getProtocol();
    }

    /**
     * 返回当前连接状态描述，例如："已连接 192.168.1.100:8080"
     */
    default String getConnectionInfo() {
        return isAlive() ? "运行中" : "已断开";
    }

    /**
     * 返回插件扩展配置的动态参数定义，用于前端按类型渲染表单。
     * <p>每一项建议使用以下字段（均为可选，前后端会做兼容映射）：</p>
     * <ul>
     *   <li>{@code code} — 参数键，写入 {@code za_sys_platform.config} JSON（与旧字段 {@code key} 等价）</li>
     *   <li>{@code name} — 显示名称（与旧字段 {@code label} 等价）</li>
     *   <li>{@code desc} — 说明文案，前端以悬浮提示展示（也可用 {@code remark} / {@code description}）</li>
     *   <li>{@code type} — 控件类型：
     *     {@code string}/{@code text} 单行文本、{@code textarea} 多行、{@code password} 密码、
     *     {@code number}/{@code integer} 数字、{@code boolean}/{@code switch} 开关、{@code select} 下拉、
     *     {@code date} 日期、{@code datetime} 日期时间</li>
     *   <li>{@code defaultValue} — 当配置中不存在该键时的默认值</li>
     *   <li>{@code placeholder} — 占位提示</li>
     *   <li>{@code required} — 是否必填</li>
     *   <li>{@code options} — select 选项列表，元素形如 {@code {"label":"显示","value":"值"}}</li>
     *   <li>{@code min} / {@code max} / {@code step} — 数字输入边界与步长</li>
     *   <li>{@code format} / {@code valueFormat} — 日期类控件的展示与绑定格式（可选）</li>
     *   <li>{@code rows} — textarea 行数</li>
     * </ul>
     */
    default List<java.util.Map<String, Object>> getConfigSchema() {
        return Collections.emptyList();
    }

    /** 厂商名称 */
    default String getVendor() {
        return "";
    }

    /** 插件版本 */
    default String getVersion() {
        return "1.0.0";
    }

    /**
     * 能力声明：alarm / monitor / device / control 等
     */
    default List<String> getCapabilities() {
        return java.util.Arrays.asList("alarm", "monitor", "device", "control");
    }
}
