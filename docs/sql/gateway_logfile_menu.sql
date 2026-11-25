-- 运维中心：历史日志 / 实时日志
-- 在 zhian_gateway 库执行。管理员账号本来就能看到全部菜单；
-- 下面会把新菜单授给已经拥有「服务监控」权限的角色。

INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
   menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '运维中心', 0, 80, 'ops', NULL, NULL, 1, 0,
       'M', '0', '0', '', 'monitor', 'admin', NOW(), '运行日志'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu WHERE parent_id = 0 AND path = 'ops' AND menu_type = 'M'
);

INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
   menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '历史日志', p.menu_id, 1, 'history', 'monitor/logfile/history', NULL, 1, 0,
       'C', '0', '0', 'monitor:logfile:list', 'log', 'admin', NOW(), '按文件查看运行日志'
FROM sys_menu p
WHERE p.parent_id = 0 AND p.path = 'ops' AND p.menu_type = 'M'
  AND NOT EXISTS (SELECT 1 FROM sys_menu c WHERE c.component = 'monitor/logfile/history')
LIMIT 1;

INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
   menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '实时日志', p.menu_id, 2, 'realtime', 'monitor/logfile/realtime', NULL, 1, 0,
       'C', '0', '0', 'monitor:logfile:list', 'online', 'admin', NOW(), '实时滚动查看运行日志'
FROM sys_menu p
WHERE p.parent_id = 0 AND p.path = 'ops' AND p.menu_type = 'M'
  AND NOT EXISTS (SELECT 1 FROM sys_menu c WHERE c.component = 'monitor/logfile/realtime')
LIMIT 1;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.menu_id
FROM sys_role_menu rm
JOIN sys_menu src ON src.menu_id = rm.menu_id AND src.perms = 'monitor:server:list'
JOIN sys_menu m ON m.parent_id = 0 AND m.path = 'ops' AND m.menu_type = 'M'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = m.menu_id
);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.menu_id
FROM sys_role_menu rm
JOIN sys_menu src ON src.menu_id = rm.menu_id AND src.perms = 'monitor:server:list'
JOIN sys_menu m ON m.perms = 'monitor:logfile:list'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = m.menu_id
);
