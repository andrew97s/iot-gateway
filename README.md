# iot-gateway

## 设备类型（FT 平台整合）

`docs/device-type/current-merged.txt` 是当前系统设备类型目录，已覆盖 FT 平台全部设备类型。

- 格式：`id|code|name|ft_code`
- 报告：`docs/device-type/mapping-report.md`
- 导入 SQL：`docs/sql/za_device_type_ft_seed.sql`（全量）或 `docs/sql/za_device_type_ft_delta.sql`（增量）
- 重新生成：`python3 scripts/merge_ft_device_types.py`
