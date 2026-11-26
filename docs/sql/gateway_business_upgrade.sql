-- ============================================================
-- 物联网关 · 核心业务配置升级脚本（MySQL）
-- 内容：
--   1. 三个业务类型配置表：告警类型 / 设备类型 / 监测类型（含插件别名映射）
--   2. 上级平台连接配置表（多上级平台：HTTP / Redis / RabbitMQ）
--   3. za_sys_message 增加统一消息字段（记录原始报文 + 转换后统一消息）
--   4. 菜单注册与示例数据
-- 执行前请备份数据库。
-- ============================================================

-- ------------------------------------------------------------
-- 1. 标准告警类型
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS za_alarm_type (
  id          BIGINT       NOT NULL                COMMENT '主键',
  code        VARCHAR(64)  NOT NULL                COMMENT '类型编码（唯一）',
  name        VARCHAR(128) NOT NULL                COMMENT '类型名称',
  level       INT          DEFAULT 2               COMMENT '告警级别：1提示 2一般 3严重 4紧急',
  aliases     TEXT                                 COMMENT '插件别名映射JSON：[{"pfCode":"jb","alias":"4"}]',
  status      CHAR(1)      DEFAULT '1'             COMMENT '状态：1启用 0停用',
  remark      VARCHAR(500)                         COMMENT '备注',
  create_time DATETIME                             COMMENT '创建时间',
  update_time DATETIME                             COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_alarm_type_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标准告警类型';

-- ------------------------------------------------------------
-- 2. 标准设备类型
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS za_device_type (
  id          BIGINT       NOT NULL                COMMENT '主键',
  code        VARCHAR(64)  NOT NULL                COMMENT '类型编码（唯一）',
  name        VARCHAR(128) NOT NULL                COMMENT '类型名称',
  aliases     TEXT                                 COMMENT '插件别名映射JSON（多个插件别名可对应同一类型）',
  status      CHAR(1)      DEFAULT '1'             COMMENT '状态：1启用 0停用',
  remark      VARCHAR(500)                         COMMENT '备注',
  create_time DATETIME                             COMMENT '创建时间',
  update_time DATETIME                             COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_device_type_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标准设备类型';

-- ------------------------------------------------------------
-- 3. 标准监测类型
--    valueType=enum   枚举值（在线/离线、开关量等，enum_options 定义取值）
--    valueType=linear 线性值（压力/液位/液压/电流/电压/信号强度/电量等，unit 为单位）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS za_monitor_type (
  id           BIGINT       NOT NULL               COMMENT '主键',
  code         VARCHAR(64)  NOT NULL               COMMENT '类型编码（唯一）',
  name         VARCHAR(128) NOT NULL               COMMENT '类型名称',
  value_type   VARCHAR(16)  DEFAULT 'linear'       COMMENT '值类型：enum枚举 / linear线性',
  unit         VARCHAR(32)                         COMMENT '监测单位（线性值）',
  enum_options TEXT                                COMMENT '枚举值定义JSON：[{"value":"1","label":"在线"}]',
  aliases      TEXT                                COMMENT '插件别名映射JSON',
  status       CHAR(1)      DEFAULT '1'            COMMENT '状态：1启用 0停用',
  remark       VARCHAR(500)                        COMMENT '备注',
  create_time  DATETIME                            COMMENT '创建时间',
  update_time  DATETIME                            COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_monitor_type_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标准监测类型';

-- ------------------------------------------------------------
-- 4. 上级平台连接配置（多上级平台）
--    push_type=url   HTTP/HTTPS 直推  config: {"pushUrls":"http://a\nhttp://b"}
--    push_type=redis Redis 队列       config: {"ip":"","port":"6379","password":"","db":"6"}
--    push_type=mq    RabbitMQ 队列    config: {"ip":"","port":5672,"vhost":"/","username":"","password":"","exchange":"za","queue":"za_monitor","key":"za"}
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS za_sys_upstream (
  id          BIGINT       NOT NULL                COMMENT '主键',
  name        VARCHAR(128) NOT NULL                COMMENT '平台名称',
  code        VARCHAR(64)  NOT NULL                COMMENT '平台代码（唯一）',
  push_type   VARCHAR(16)  NOT NULL                COMMENT '推送方式：url / redis / mq',
  config      TEXT                                 COMMENT '连接配置JSON',
  status      CHAR(1)      DEFAULT '1'             COMMENT '状态：1启用 0停用',
  remark      VARCHAR(500)                         COMMENT '备注',
  create_time DATETIME                             COMMENT '创建时间',
  update_time DATETIME                             COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_upstream_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上级平台连接配置';

-- （可选）从旧的 gateway 插件单通道配置迁移到 za_sys_upstream：
-- INSERT INTO za_sys_upstream (id, name, code, push_type, config, status, create_time)
-- SELECT 1, '默认上级平台', 'default-upstream',
--        COALESCE(JSON_UNQUOTE(JSON_EXTRACT(p.config, '$.pushType')), 'mq'),
--        p.config, p.status, NOW()
-- FROM za_sys_platform p WHERE p.code = 'gateway'
--   AND NOT EXISTS (SELECT 1 FROM za_sys_upstream);

-- ------------------------------------------------------------
-- 5. 消息表增加统一消息字段（原始报文存 content，统一消息存 unified_content）
-- ------------------------------------------------------------
ALTER TABLE za_sys_message ADD COLUMN unified_content TEXT COMMENT '转换后的统一消息JSON' AFTER content;

-- ------------------------------------------------------------
-- 6. 菜单注册（挂到与设备管理相同的父目录下）
-- ------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '告警类型', m.parent_id, 60, 'alarm-type', 'gateway/alarmtype/index', NULL, 1, 0, 'C', '0', '0', 'sys:alarmtype:list', 'alert', 'admin', NOW(), '标准告警类型与插件别名映射'
FROM sys_menu m WHERE m.component = 'gateway/device/index'
  AND NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.component = 'gateway/alarmtype/index') LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '设备类型', m.parent_id, 61, 'device-type', 'gateway/devicetype/index', NULL, 1, 0, 'C', '0', '0', 'sys:devicetype:list', 'component', 'admin', NOW(), '标准设备类型与插件别名映射'
FROM sys_menu m WHERE m.component = 'gateway/device/index'
  AND NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.component = 'gateway/devicetype/index') LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '监测类型', m.parent_id, 62, 'monitor-type', 'gateway/monitortype/index', NULL, 1, 0, 'C', '0', '0', 'sys:monitortype:list', 'chart', 'admin', NOW(), '标准监测类型（枚举/线性）与插件别名映射'
FROM sys_menu m WHERE m.component = 'gateway/device/index'
  AND NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.component = 'gateway/monitortype/index') LIMIT 1;

-- ------------------------------------------------------------
-- 7. 示例数据（可按需调整）
-- ------------------------------------------------------------
INSERT IGNORE INTO za_alarm_type (id, code, name, level, aliases, status, create_time) VALUES
 (101, 'fire',          '火警',     4, '[{"pfCode":"jb","alias":"4"},{"pfCode":"hikvision","alias":"fireAlarm"}]', '1', NOW()),
 (102, 'fault',         '故障',     2, '[{"pfCode":"jb","alias":"5"}]',        '1', NOW()),
 (103, 'smoke',         '烟雾告警', 3, '[{"pfCode":"jbox","alias":"smoke"}]',  '1', NOW()),
 (104, 'water-leak',    '水浸告警', 3, NULL, '1', NOW()),
 (105, 'intrusion',     '入侵告警', 3, '[{"pfCode":"hikvision","alias":"fielddetection"}]', '1', NOW()),
 (106, 'offline-alarm', '离线告警', 1, NULL, '1', NOW());

INSERT IGNORE INTO za_device_type (id, code, name, aliases, status, create_time) VALUES
 (201, 'camera',        '摄像机',     '[{"pfCode":"hikvision","alias":"IPC"},{"pfCode":"dhsdk","alias":"ipc"}]', '1', NOW()),
 (202, 'smoke-detector','烟感探测器', '[{"pfCode":"jb","alias":"11"},{"pfCode":"jbox","alias":"smoke"}]',        '1', NOW()),
 (203, 'access-control','门禁',       NULL, '1', NOW()),
 (204, 'water-sensor',  '水浸探测器', NULL, '1', NOW()),
 (205, 'temp-humidity', '温湿度传感器', NULL, '1', NOW());

INSERT IGNORE INTO za_monitor_type (id, code, name, value_type, unit, enum_options, aliases, status, create_time) VALUES
 (301, 'online',      '在线状态', 'enum',   NULL,  '[{"value":"1","label":"在线"},{"value":"0","label":"离线"}]', NULL, '1', NOW()),
 (302, 'switch',      '开关量',   'enum',   NULL,  '[{"value":"1","label":"开"},{"value":"0","label":"关"}]',     NULL, '1', NOW()),
 (303, 'signal',      '信号强度', 'linear', 'dBm', NULL, '[{"alias":"rssi"}]',    '1', NOW()),
 (304, 'battery',     '电量',     'linear', '%',   NULL, NULL, '1', NOW()),
 (305, 'voltage',     '电压',     'linear', 'V',   NULL, '[{"alias":"voltage"}]', '1', NOW()),
 (306, 'current',     '电流',     'linear', 'A',   NULL, NULL, '1', NOW()),
 (307, 'pressure',    '压力',     'linear', 'MPa', NULL, NULL, '1', NOW()),
 (308, 'liquid-level','液位',     'linear', 'm',   NULL, NULL, '1', NOW()),
 (309, 'temperature', '温度',     'linear', '℃',  NULL, '[{"alias":"temperature"}]', '1', NOW());

-- 注意：示例 aliases 中省略 pfCode 表示对所有插件生效（如 signal 的 rssi 别名）。
