-- 历史日志 / 实时日志挂到「系统管理 → 日志管理」下。
-- 在 zhian_gateway 库执行。脚本可重复执行。
-- 若之前已经挂在「运维中心」，会迁过来并删掉空的运维中心。

UPDATE sys_menu c
JOIN (
    SELECT p.menu_id
    FROM sys_menu p
    JOIN sys_menu s ON s.menu_id = p.parent_id
    WHERE p.menu_name = '日志管理' AND p.menu_type = 'M'
      AND s.menu_name = '系统管理' AND s.parent_id = 0
    ORDER BY p.menu_id
    LIMIT 1
) target
SET c.parent_id = target.menu_id,
    c.order_num = IF(c.component = 'monitor/logfile/history', 3, 4)
WHERE c.component IN ('monitor/logfile/history', 'monitor/logfile/realtime');

INSERT INTO sys_menu
(menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
 menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '历史日志', p.menu_id, 3, 'history', 'monitor/logfile/history', NULL, 1, 0,
       'C', '0', '0', 'monitor:logfile:list', 'log', 'admin', NOW(), '按文件查看运行日志'
FROM sys_menu p
JOIN sys_menu s ON s.menu_id = p.parent_id
WHERE p.menu_name = '日志管理' AND p.menu_type = 'M'
  AND s.menu_name = '系统管理' AND s.parent_id = 0
  AND NOT EXISTS (SELECT 1 FROM sys_menu c WHERE c.component = 'monitor/logfile/history')
LIMIT 1;

INSERT INTO sys_menu
(menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
 menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '实时日志', p.menu_id, 4, 'realtime', 'monitor/logfile/realtime', NULL, 1, 0,
       'C', '0', '0', 'monitor:logfile:list', 'online', 'admin', NOW(), '实时滚动查看运行日志'
FROM sys_menu p
JOIN sys_menu s ON s.menu_id = p.parent_id
WHERE p.menu_name = '日志管理' AND p.menu_type = 'M'
  AND s.menu_name = '系统管理' AND s.parent_id = 0
  AND NOT EXISTS (SELECT 1 FROM sys_menu c WHERE c.component = 'monitor/logfile/realtime')
LIMIT 1;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.menu_id
FROM sys_role_menu rm
JOIN sys_menu src ON src.menu_id = rm.menu_id
    AND src.perms IN ('monitor:server:list', 'monitor:logfile:list')
JOIN sys_menu m ON m.component IN ('monitor/logfile/history', 'monitor/logfile/realtime')
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = m.menu_id
);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, p.menu_id
FROM sys_role_menu rm
JOIN sys_menu src ON src.menu_id = rm.menu_id AND src.perms = 'monitor:logfile:list'
JOIN sys_menu p ON p.menu_id = src.parent_id AND p.menu_name = '日志管理' AND p.menu_type = 'M'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = p.menu_id
);

DELETE FROM sys_role_menu
WHERE menu_id IN (
    SELECT menu_id FROM (
        SELECT m.menu_id
        FROM sys_menu m
        WHERE m.parent_id = 0 AND m.path = 'ops' AND m.menu_name = '运维中心'
          AND NOT EXISTS (SELECT 1 FROM sys_menu c WHERE c.parent_id = m.menu_id)
    ) t
);

DELETE FROM sys_menu
WHERE menu_id IN (
    SELECT menu_id FROM (
        SELECT m.menu_id
        FROM sys_menu m
        WHERE m.parent_id = 0 AND m.path = 'ops' AND m.menu_name = '运维中心'
          AND NOT EXISTS (SELECT 1 FROM sys_menu c WHERE c.parent_id = m.menu_id)
    ) t
);
