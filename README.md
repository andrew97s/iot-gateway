# 物联网网关（IoT Gateway）原型设计

多厂商、多协议物联设备统一接入网关的**原型设计**（设计文档 + 静态页面原型，不含代码实现）。

## 内容

- **设计文档**：[`docs/iot-gateway-design.md`](docs/iot-gateway-design.md)
  总体架构、插件化接入框架、统一消息模型、上级平台同步、数据模型、API 设计等。
- **页面原型**：`prototype/` 目录，纯静态 HTML/CSS（少量 JS 用于页签/弹窗演示），浏览器直接打开即可，无需构建环境。

| 页面 | 文件 | 说明 |
|------|------|------|
| 首页概览 | [`prototype/index.html`](prototype/index.html) | 指标卡、消息趋势、插件/平台状态、最新告警、系统资源 |
| 设备管理 | [`prototype/devices.html`](prototype/devices.html) | 设备列表、修改/删除、指定设备同步至上级平台 |
| 插件管理 | [`prototype/plugins.html`](prototype/plugins.html) | 插件启停、动态参数配置、消息统计、运行日志 |
| 消息日志 | [`prototype/messages.html`](prototype/messages.html) | 消息类型、原始/统一报文对照、按平台的同步结果与重推 |
| 系统配置 | [`prototype/settings.html`](prototype/settings.html) | IP 配置、系统核心参数、上级平台配置（推送方式动态表单） |

## 快速查看

```bash
# 任选其一
open prototype/index.html            # macOS
python3 -m http.server -d prototype  # 或起一个静态服务后访问 http://localhost:8000
```
