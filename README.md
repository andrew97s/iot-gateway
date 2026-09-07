# 物联网网关（IoT Gateway）

多厂商、多协议物联设备统一接入网关：南向插件化接入（海康 / 大华 / 青鸟 / Modbus / MQTT / GB28181 等），
消息统一格式化后同步上级平台（HTTP / MQ / Redis / WebSocket 级联），内置 Web 管理控制台。

## 仓库结构

| 目录 | 说明 |
|------|------|
| `gateway-backend/` | 后端（Spring Boot 2.5 + MyBatis-Plus + MySQL/SQLite，Java 8+） |
| `gateway-web/` | 前端管理控制台（Vue 3 + Vite + Element Plus，pnpm） |
| `prototype/` | 静态页面原型（纯 HTML/CSS，双击打开），管理页 UI 的设计基准 |
| `docs/iot-gateway-design.md` | 原型设计文档（架构 / 插件框架 / 统一消息模型 / 数据模型 / API） |
| `docs/sql/gateway_menu_upgrade.sql` | 菜单与配置升级 SQL（新增「系统配置」页等） |
| `docs/sql/gateway_business_upgrade.sql` | 业务配置升级 SQL（告警/设备/监测类型表、上级平台表、消息表统一报文字段、菜单与示例数据） |

## 管理控制台页面（对齐 `prototype/` 原型）

| 页面 | 前端组件 | 主要后端接口 |
|------|----------|----------------|
| 首页概览 | `views/home/index.vue` | `GET /sys/overview`（聚合：设备在线/今日消息/告警/插件状态/24h趋势/类型分布/最新告警）、`GET /monitor/server` |
| 设备管理 | `views/gateway/device/index.vue` | `/sys/device/*`（CRUD/导入导出/在线统计）、`GET /api/device/push`（指定设备同步上级） |
| 插件管理 | `views/gateway/platform/index.vue` | `/sys/platform/*`（启停/动态配置Schema/运行统计/运行日志） |
| 消息日志 | `views/gateway/message/index.vue` | `/sys/message/*`（筛选/详情[原始报文+统一消息对照]/重推/推送记录） |
| 告警类型 | `views/gateway/alarmtype/index.vue` | `/sys/alarm-type/*`（编码/名称/级别/插件别名映射） |
| 设备类型 | `views/gateway/devicetype/index.vue` | `/sys/device-type/*`（编码/名称/插件别名映射，多别名可对应同一类型） |
| 监测类型 | `views/gateway/monitortype/index.vue` | `/sys/monitor-type/*`（枚举值/线性值、单位、枚举定义、插件别名映射） |
| 系统配置 | `views/gateway/settings/index.vue` | `GET /sys/network/interfaces`、`GET/PUT /sys/setting/params`、`/sys/upstream/*`（多上级平台：HTTP/Redis/RabbitMQ + 连接测试）、`/sys/cascade/*`（级联上级） |

## 统一消息格式（同步上级平台）

消息类型四种：`device`（设备增删改查事件）、`alarm`（标准告警：类型/时间/设备/状态 发生 occur / 撤销 cancel + 原因）、`monitor`（标准监测：类型/单位/值/时间）、`control`（上级平台反控指令）。

- 各插件产出的原始事件在同步边界由 `UnifiedMessageConverter` 统一转换：厂商告警码/设备类型/监测项经三张类型配置表的**插件别名映射**解析为标准类型。
- 信封含全局唯一 `messageId`，上级平台按其去重实现**幂等**；推送失败自动标记 `failed` 并支持重推（保持原 messageId），保证**可靠性**。
- 消息日志同时记录**原始报文**（`content`）与**转换后统一消息**（`unified_content`），以及每个上级平台的逐条同步结果（推送记录）。

> 菜单由后端 `sys_menu` 动态下发，新增页面后需执行 `docs/sql/gateway_menu_upgrade.sql` 注册菜单。

## 本地启动

### 后端

```bash
cd gateway-backend
# JDK 8/11 + Maven；dev 环境使用 MySQL（application-dev.yml），端口 9200
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# 或单机模式（内嵌 SQLite）：-Dspring-boot.run.profiles=single
```

### 前端

```bash
cd gateway-web
pnpm install
pnpm dev        # 端口 9103，代理目标见 .env.development 的 VITE_APP_PROXY_API
pnpm build:production
```

### 静态原型

```bash
python3 -m http.server 8000 -d prototype   # 或直接双击 prototype/index.html
```
