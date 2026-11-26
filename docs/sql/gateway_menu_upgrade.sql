-- ============================================================
-- 物联网关 · 原型整合菜单升级脚本（MySQL）
-- 说明：
--   1. 菜单为后端 sys_menu 动态加载，本脚本负责：
--      a) 新增「系统配置」页面菜单（gateway/settings/index）
--      b) 将既有菜单名称与原型统一（平台管理 -> 插件管理 等）
--   2. 请在 zhian_gateway 数据库执行；执行前建议备份 sys_menu。
-- ============================================================

-- 1) 新增「系统配置」菜单（挂到与设备管理相同的父目录下）
INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
   menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT
  '系统配置', m.parent_id, 90, 'settings', 'gateway/settings/index', NULL, 1, 0,
  'C', '0', '0', 'system:config:list', 'system', 'admin', NOW(), 'IP配置/核心参数/上级平台配置'
FROM sys_menu m
WHERE m.component = 'gateway/device/index'
  AND NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.component = 'gateway/settings/index')
LIMIT 1;

-- 2) 菜单名称与原型统一（按组件路径定位，避免依赖菜单ID）
UPDATE sys_menu SET menu_name = '首页概览'   WHERE component = 'home/index'             AND menu_type = 'C';
UPDATE sys_menu SET menu_name = '设备管理'   WHERE component = 'gateway/device/index'   AND menu_type = 'C';
UPDATE sys_menu SET menu_name = '插件管理'   WHERE component = 'gateway/platform/index' AND menu_type = 'C';
UPDATE sys_menu SET menu_name = '消息日志'   WHERE component = 'gateway/message/index'  AND menu_type = 'C';

-- 3)（可选）为管理员之外的角色授权新菜单：
-- INSERT INTO sys_role_menu (role_id, menu_id)
-- SELECT r.role_id, m.menu_id FROM sys_role r, sys_menu m
-- WHERE r.role_key = '<你的角色key>' AND m.component = 'gateway/settings/index';

-- 4)（可选）核心参数默认值（首次访问「系统配置-核心参数」并保存时会自动写入，
--    也可以预先初始化）：
-- INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time)
-- VALUES
--   ('网关名称', 'gateway.name', '物联网关', 'N', 'admin', NOW()),
--   ('网关编码（同步上级平台时的网关标识）', 'gateway.code', 'GW-0001', 'N', 'admin', NOW()),
--   ('设备离线判定阈值（秒）', 'gateway.offline.threshold', '300', 'N', 'admin', NOW()),
--   ('插件异常自动重启', 'gateway.plugin.autorestart', 'true', 'N', 'admin', NOW()),
--   ('新设备自动同步上级平台', 'gateway.device.autosync', 'false', 'N', 'admin', NOW());
