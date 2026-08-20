#!/usr/bin/env python3
"""Merge FT platform device types into the current-system catalog.

current line format: id|code|name|ft_code
ft source: JSON {code, name, facilitiesCode}

Rules:
  1. Keep every existing current row (id / code / name unchanged).
  2. If current.ft_code is empty and the name exactly matches an FT type,
     fill ft_code.
  3. If current.ft_code is not in the FT catalog, but the name exactly
     matches an unmapped FT type, update ft_code to that FT code (stale mapping).
  4. Remaining FT types become new current rows (new snowflake id + code).
"""

from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CURRENT_PATH = ROOT / "data" / "current-device-types.txt"
FT_PATH = ROOT / "data" / "ft-device-types.json"
OUT_MERGED = ROOT / "docs" / "device-type" / "current-merged.txt"
OUT_NEW = ROOT / "docs" / "device-type" / "new-types.txt"
OUT_JSON = ROOT / "docs" / "device-type" / "device-types.json"
OUT_REPORT = ROOT / "docs" / "device-type" / "mapping-report.md"
OUT_SQL = ROOT / "docs" / "sql" / "za_device_type_ft_seed.sql"
OUT_DELTA = ROOT / "docs" / "sql" / "za_device_type_ft_delta.sql"

# New current ids: 220000000000000 + FT code (stable, larger than existing snowflakes).
NEW_ID_BASE = 220_000_000_000_000

# Proposed current-system codes for newly inserted FT types.
# Existing current codes are never reused.
NEW_TYPE_CODES = {
    0: "GENERAL",       # 通用
    11: "PCGD",         # 点型可燃气体探测器
    12: "DICGD",        # 独立式可燃气体探测器
    13: "LCGD",         # 线型可燃气体探测器
    25: "FDET",         # 火灾探测器
    30: "TFD",          # 感温火灾探测器
    32: "PTTFDS",       # 点型感温火灾探测器(S型)
    33: "PTTFDR",       # 点型感温火灾探测器(R型)
    35: "LTFDS",        # 线型感温火灾探测器(S型)
    36: "LTFDR",        # 线型感温火灾探测器(R型)
    40: "SFD",          # 感烟火灾探测器
    41: "PLISFD",       # 点型离子感烟火灾探测器
    50: "CFD",          # 复合式火灾探测器
    52: "CPTHFD",       # 复合式感光感温火灾探浏器
    74: "GD",           # 气体探测器
    84: "MOD",          # 模块
    86: "OM",           # 输出模块
    88: "RELAYM",       # 中继模块
    89: "SCIM",         # 短路隔离模块
    101: "VDA",         # 阀驱动装置
    102: "FDOOR",       # 防火门
    104: "HVAC",        # 通风空调
    111: "SMEFAN",      # 防烟排烟风机
    117: "FDC",         # 防火门控制器
    121: "ALDEV",       # 警报装置
    136: "EBCAST",      # 紧急广播
    137: "BBCAST",      # 总线广播
    145: "EL",          # 应急照明
    148: "OCD",         # 过电流探测器
    149: "TRR",         # 脱扣继电器
    152: "CMM",         # 电流监控模块
    153: "IFACE",       # 接口
    157: "SW",          # 开关
    161: "LCB",         # 回路板
    162: "BPANEL",      # 总线盘
    163: "MLP",         # 多线盘
    166: "OBC",         # 其他板卡
    176: "DCL",         # 闭门器
    200: "COM",         # 控制输出模块
    201: "IORD",        # IOR式探测器
    203: "ETD",         # 环境温度探测器
    215: "RPDV",        # 余压风阀
    218: "FEDB",        # 消防应急配电箱
    219: "FEL",         # 消防应急灯具
    222: "FEZ",         # 灭火区
    223: "FEA2",        # 灭火辅助（与 FEA/135 同名的另一编码）
    266: "PNP",         # 管网压力
    267: "STKP",        # 稳压罐压力
    268: "LPD",         # 防漏水检测
    271: "WTAV",        # 水箱(池)报警阀
    280: "INFEQ",       # 充气设备
    281: "PAP",         # 管道气压
    286: "SA",          # 声报警器
    291: "SNOFDMM",     # 单常开防火门模块（current SNOFDM 已映射 141）
    292: "DNOFDMM",     # 双常开防火门模块（current DNOFDM 已映射 143）
    293: "SNCFDMM",     # 单常闭防火门模块（current SNCFDM 已映射 142）
    294: "DNCFDMM",     # 双常闭防火门模块（current DNCFDM 已映射 144）
    296: "MPPEFMD",     # 测量热解粒子式电气火灾监控探测器
    308: "RMS",         # 远程监控系统
    312: "AVTEDV",      # 报警阀组试验排放阀
    337: "WTM",         # 无线传输模块
    344: "BFTM",        # 总线消防电话主机
    349: "NGC",         # 无网关控制器
    350: "EFME",        # 电气火灾监控设备
    352: "PHSFD",       # 点型家用感烟火灾探测器
    361: "SU",          # 信号单元
    362: "HTCABLE",     # 感温电缆
    363: "HTPOS",       # 感温位置
    364: "HTZ",         # 感温分区
    366: "GFIOTMD",     # 气体灭火物联网实时监测装置
    367: "IPCAM",       # 摄像头（camera 已用于摄像机）
    371: "WPM",         # 无线压力监测
    375: "NOFD",        # 常开防火门
    376: "NCFD",        # 常闭防火门
    378: "IEUT",        # 智能用电终端
    379: "SSUED",       # 智慧安全用电设备
    383: "EQC",         # 环境质量控制器
    386: "WSC",         # 水系统控制器
    387: "CCAB",        # 充电柜
    388: "CCOM",        # 充电仓
    389: "CCS",         # 充电柜屏幕
    390: "ICCGD",       # 工商业用可燃气体探测器
    391: "WSMDA",       # 无线智能门磁报警器
    392: "ICBCM",       # 智能断路器通讯模组
    393: "PPD",         # 热解粒子探测器
    394: "WMD",         # 细水雾探测器
    395: "WFHB",        # 无线消火栓按钮
    396: "WRGVLD",      # 无线燃气阀井气体泄漏检测仪
}


def parse_current(path: Path) -> list[dict]:
    rows = []
    for line in path.read_text(encoding="utf-8").splitlines():
        raw = line.rstrip("\n")
        if not raw.strip():
            continue
        parts = raw.split("|")
        cid, code, name = parts[0], parts[1], parts[2]
        ft_code = parts[3] if len(parts) > 3 else ""
        if ft_code == '""':
            ft_code = ""
        rows.append(
            {
                "id": cid,
                "code": code,
                "name": name,
                "ft_code": ft_code,
                "raw": raw,
                "source": "current",
                "action": "keep",
            }
        )
    return rows


def parse_ft(path: Path) -> list[dict]:
    payload = json.loads(path.read_text(encoding="utf-8"))
    return payload["data"]


def new_current_id(ft_code: int) -> int:
    return NEW_ID_BASE + int(ft_code)


def sql_escape(value: str) -> str:
    return value.replace("\\", "\\\\").replace("'", "''")


def aliases_json(ft_code: str) -> str | None:
    if not ft_code:
        return None
    return json.dumps(
        [{"pfCode": "ft", "alias": str(ft_code)}],
        ensure_ascii=False,
        separators=(",", ":"),
    )


def sql_value_tuple(row: dict, ft_by_code: dict) -> str:
    aliases = aliases_json(row["ft_code"])
    aliases_sql = "NULL" if aliases is None else "'" + sql_escape(aliases) + "'"
    remark_parts = []
    if row["action"] == "insert":
        remark_parts.append("source=ft")
    if row["ft_code"] in ft_by_code:
        fac = ft_by_code[row["ft_code"]].get("facilitiesCode")
        if fac is not None:
            remark_parts.append(f"ft.facilitiesCode={fac}")
    if row.get("old_ft_code"):
        remark_parts.append(f"replacedStaleFtCode={row['old_ft_code']}")
    remark_sql = (
        "NULL" if not remark_parts else "'" + sql_escape("; ".join(remark_parts)) + "'"
    )
    return (
        "({id}, '{code}', '{name}', {aliases}, '1', {remark}, NOW(), NOW())".format(
            id=row["id"],
            code=sql_escape(row["code"]),
            name=sql_escape(row["name"]),
            aliases=aliases_sql,
            remark=remark_sql,
        )
    )


def main() -> None:
    current = parse_current(CURRENT_PATH)
    ft_list = parse_ft(FT_PATH)
    ft_by_code = {str(item["code"]): item for item in ft_list}
    ft_by_name: dict[str, list[dict]] = {}
    for item in ft_list:
        ft_by_name.setdefault(item["name"], []).append(item)

    used_codes = {row["code"] for row in current}
    mapped_ft = {row["ft_code"] for row in current if row["ft_code"]}

    filled: list[dict] = []
    updated: list[dict] = []

    # Pass 1: fill empty ft_code by exact name.
    still_unmapped = [
        item for item in ft_list if str(item["code"]) not in mapped_ft
    ]
    unmapped_by_name: dict[str, list[dict]] = {}
    for item in still_unmapped:
        unmapped_by_name.setdefault(item["name"], []).append(item)

    for row in current:
        if row["ft_code"]:
            continue
        candidates = unmapped_by_name.get(row["name"], [])
        if not candidates:
            continue
        item = candidates.pop(0)
        row["ft_code"] = str(item["code"])
        row["action"] = "fill"
        mapped_ft.add(row["ft_code"])
        filled.append(row)

    # Pass 2: replace stale ft_code (not in FT catalog) when name matches.
    still_unmapped = [
        item for item in ft_list if str(item["code"]) not in mapped_ft
    ]
    unmapped_by_name = {}
    for item in still_unmapped:
        unmapped_by_name.setdefault(item["name"], []).append(item)

    for row in current:
        if not row["ft_code"] or row["ft_code"] in ft_by_code:
            continue
        candidates = unmapped_by_name.get(row["name"], [])
        if not candidates:
            continue
        item = candidates.pop(0)
        old = row["ft_code"]
        row["ft_code"] = str(item["code"])
        row["action"] = "update-stale"
        row["old_ft_code"] = old
        mapped_ft.add(row["ft_code"])
        updated.append(row)

    # Pass 3: remaining FT types become new current rows.
    remaining = [item for item in ft_list if str(item["code"]) not in mapped_ft]
    missing_code_map = [item for item in remaining if item["code"] not in NEW_TYPE_CODES]
    if missing_code_map:
        raise SystemExit(
            "NEW_TYPE_CODES missing FT codes: "
            + ", ".join(str(item["code"]) for item in missing_code_map)
        )

    collisions = [
        (item["code"], NEW_TYPE_CODES[item["code"]])
        for item in remaining
        if NEW_TYPE_CODES[item["code"]] in used_codes
    ]
    if collisions:
        raise SystemExit(f"code collisions with current catalog: {collisions}")

    existing_ids = {int(row["id"]) for row in current}
    new_rows: list[dict] = []
    for item in remaining:
        new_id = new_current_id(item["code"])
        if new_id in existing_ids:
            raise SystemExit(f"generated id collides with current catalog: {new_id}")
        existing_ids.add(new_id)
        code = NEW_TYPE_CODES[item["code"]]
        used_codes.add(code)
        row = {
            "id": str(new_id),
            "code": code,
            "name": item["name"],
            "ft_code": str(item["code"]),
            "source": "ft",
            "action": "insert",
            "facilitiesCode": item.get("facilitiesCode"),
        }
        new_rows.append(row)
        mapped_ft.add(row["ft_code"])

    merged = current + new_rows
    if len(mapped_ft & set(ft_by_code)) != len(ft_by_code):
        missing = sorted(set(ft_by_code) - mapped_ft, key=lambda x: int(x))
        raise SystemExit(f"FT codes still unmapped: {missing}")

    # Write merged catalog (same 4-column format as current).
    merged_lines = []
    for row in merged:
        ft_code = row["ft_code"]
        merged_lines.append(f"{row['id']}|{row['code']}|{row['name']}|{ft_code}")
    OUT_MERGED.write_text("\n".join(merged_lines) + "\n", encoding="utf-8")
    OUT_NEW.write_text(
        "\n".join(
            f"{row['id']}|{row['code']}|{row['name']}|{row['ft_code']}"
            for row in new_rows
        )
        + "\n",
        encoding="utf-8",
    )

    payload = {
        "total": len(merged),
        "kept": sum(1 for row in current if row["action"] == "keep"),
        "filled": len(filled),
        "updatedStale": len(updated),
        "inserted": len(new_rows),
        "items": [
            {
                "id": row["id"],
                "code": row["code"],
                "name": row["name"],
                "ftCode": row["ft_code"] or None,
                "action": row["action"],
                "oldFtCode": row.get("old_ft_code"),
                "facilitiesCode": (
                    ft_by_code[row["ft_code"]].get("facilitiesCode")
                    if row["ft_code"] in ft_by_code
                    else row.get("facilitiesCode")
                ),
            }
            for row in merged
        ],
    }
    OUT_JSON.write_text(
        json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )

    # SQL seed for za_device_type (aliases.pfCode = ft).
    sql_lines = [
        "-- ============================================================",
        "-- 将 current 设备类型（含已整合的 FT 平台类型）写入 za_device_type",
        "-- aliases: [{\"pfCode\":\"ft\",\"alias\":\"<FT code>\"}]",
        "-- 以 code 为唯一键：已存在则更新名称与 aliases，不存在则插入。",
        "-- ============================================================",
        "",
        "INSERT INTO za_device_type (id, code, name, aliases, status, remark, create_time, update_time)",
        "VALUES",
    ]
    value_sql = ["  " + sql_value_tuple(row, ft_by_code) for row in merged]
    sql_lines.append(",\n".join(value_sql))
    sql_lines.append(
        "ON DUPLICATE KEY UPDATE "
        "name = VALUES(name), "
        "aliases = VALUES(aliases), "
        "status = VALUES(status), "
        "remark = VALUES(remark), "
        "update_time = NOW();"
    )
    sql_lines.append("")
    OUT_SQL.write_text("\n".join(sql_lines), encoding="utf-8")

    changed_existing = filled + updated
    delta = [
        "-- ============================================================",
        "-- FT 设备类型增量脚本：补全/更新已有映射，并插入未覆盖的 FT 类型",
        "-- 适用于已经导入 current 目录的数据库。",
        "-- ============================================================",
        "",
    ]
    if changed_existing:
        delta.append("-- 1. 补全空映射 / 更新已不存在的 FT code")
        for row in changed_existing:
            aliases = aliases_json(row["ft_code"])
            aliases_sql = "'" + sql_escape(aliases) + "'"
            comment = ""
            if row.get("old_ft_code"):
                comment = f"  -- {row['name']}: {row['old_ft_code']} -> {row['ft_code']}"
            else:
                comment = f"  -- {row['name']}"
            delta.append(
                f"UPDATE za_device_type SET aliases = {aliases_sql}, update_time = NOW() "
                f"WHERE code = '{sql_escape(row['code'])}';{comment}"
            )
        delta.append("")
    delta.append("-- 2. 插入尚未接入的 FT 设备类型")
    delta.append(
        "INSERT INTO za_device_type (id, code, name, aliases, status, remark, create_time, update_time)"
    )
    delta.append("VALUES")
    delta.append(",\n".join("  " + sql_value_tuple(row, ft_by_code) for row in new_rows))
    delta.append("ON DUPLICATE KEY UPDATE name = VALUES(name), aliases = VALUES(aliases), update_time = NOW();")
    delta.append("")
    OUT_DELTA.write_text("\n".join(delta), encoding="utf-8")

    stale_kept = [
        row
        for row in current
        if row["ft_code"] and row["ft_code"] not in ft_by_code and row["action"] == "keep"
    ]

    def fmt_row(row: dict) -> str:
        extra = f" (原 ft_code={row['old_ft_code']})" if row.get("old_ft_code") else ""
        return f"| `{row['id']}` | `{row['code']}` | {row['name']} | `{row['ft_code']}` |{extra}"

    report = [
        "# FT 设备类型整合报告",
        "",
        "将 FT 平台设备类型整合进当前系统设备类型目录。",
        "",
        "## 统计",
        "",
        f"- current 原有类型：{len(current)}",
        f"- FT 平台类型：{len(ft_list)}",
        f"- 保持不变：{sum(1 for row in current if row['action'] == 'keep')}",
        f"- 按名称补全空 ft_code：{len(filled)}",
        f"- 名称相同且原 ft_code 已不在 FT 目录中，更新为现行 FT code：{len(updated)}",
        f"- 新增 current 类型（覆盖其余 FT code）：{len(new_rows)}",
        f"- 整合后 current 类型总数：{len(merged)}",
        f"- FT code 覆盖率：{len(ft_by_code)}/{len(ft_by_code)}",
        "",
        "## 列说明",
        "",
        "`id|code|name|ft_code`",
        "",
        "- `id`：当前系统主键",
        "- `code`：当前系统类型编码",
        "- `name`：类型名称",
        "- `ft_code`：FT 平台 `code`",
        "",
        "## 按名称补全",
        "",
        "| id | code | name | ft_code |",
        "| --- | --- | --- | --- |",
    ]
    for row in filled:
        report.append(fmt_row(row))
    if not filled:
        report.append("| （无） | | | |")

    report += [
        "",
        "## 更新过期映射",
        "",
        "原 `ft_code` 不在本次 FT 目录中，且名称与未映射 FT 类型完全一致。",
        "",
        "| id | code | name | 新 ft_code |",
        "| --- | --- | --- | --- |",
    ]
    for row in updated:
        report.append(fmt_row(row))
    if not updated:
        report.append("| （无） | | | |")

    report += [
        "",
        "## 原 ft_code 不在本次 FT 目录中（保留）",
        "",
        "这些映射在 current 中已有值，但本次 FT 接口未返回对应 code，未改动。",
        "",
        "| id | code | name | ft_code |",
        "| --- | --- | --- | --- |",
    ]
    for row in stale_kept:
        report.append(
            f"| `{row['id']}` | `{row['code']}` | {row['name']} | `{row['ft_code']}` |"
        )
    if not stale_kept:
        report.append("| （无） | | | |")

    report += [
        "",
        "## 新增类型",
        "",
        "新增行的 `id` = `220000000000000 + ft_code`（稳定可复现）。`code` 按现有目录命名习惯拟定，如需调整可只改 code、保留 id 与 ft_code。",
        "",
        "| id | code | name | ft_code |",
        "| --- | --- | --- | --- |",
    ]
    for row in new_rows:
        report.append(
            f"| `{row['id']}` | `{row['code']}` | {row['name']} | `{row['ft_code']}` |"
        )

    report += [
        "",
        "## 产出文件",
        "",
        "- [`docs/device-type/current-merged.txt`](current-merged.txt)：整合后的完整目录（与 current 相同的四列格式）",
        "- [`docs/device-type/new-types.txt`](new-types.txt)：仅新增行，便于单独导入",
        "- [`docs/device-type/device-types.json`](device-types.json)：结构化结果",
        "- [`docs/sql/za_device_type_ft_seed.sql`](../sql/za_device_type_ft_seed.sql)：全量写入 `za_device_type`",
        "- [`docs/sql/za_device_type_ft_delta.sql`](../sql/za_device_type_ft_delta.sql)：仅更新 7 条 + 插入 88 条",
        "",
    ]
    OUT_REPORT.write_text("\n".join(report), encoding="utf-8")

    print(f"current={len(current)} ft={len(ft_list)} merged={len(merged)}")
    print(f"fill={len(filled)} update={len(updated)} insert={len(new_rows)}")
    print(f"wrote {OUT_MERGED}")
    print(f"wrote {OUT_NEW}")
    print(f"wrote {OUT_JSON}")
    print(f"wrote {OUT_SQL}")
    print(f"wrote {OUT_DELTA}")
    print(f"wrote {OUT_REPORT}")


if __name__ == "__main__":
    main()
