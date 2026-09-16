package com.zhian.gateway.third.others.mk.constants;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 铭控 V3.3 设备类型（表二）。
 * <p>
 * 同一设备可上报多路监测值（压力+温度、温湿度、多通道等），每路独立单位、
 * 数据类型、LSB 和监测类型别名，供解析与 {@code za_monitor_type} 映射使用。
 *
 * @author tongwenjin
 * @since 2026/9/3
 */
@Getter
public class MkV3Type {

    public static final String KIND_PRESSURE = "pressure";
    public static final String KIND_LEVEL = "level";
    public static final String KIND_TEMP = "temp";
    public static final String KIND_HUMIDITY = "humidity";
    public static final String KIND_WATER = "water";
    public static final String KIND_HYDRANT = "hydrant";
    public static final String KIND_WELL = "wellcover";
    public static final String KIND_GAS = "gas";
    public static final String KIND_FLOW = "flow";
    public static final String KIND_SWITCH = "switch";
    public static final String KIND_OTHER = "other";

    public enum DataKind {
        INT16_S,
        INT16_U,
        INT32_S,
        INT32_U,
        FLOAT32,
        FLOAT64
    }

    @Getter
    public static final class Field {
        private final String name;
        private final String unit;
        private final DataKind kind;
        private final double lsb;
        /** 映射 za_monitor_type 的 alias / code */
        private final String monitorAlias;

        private Field(String name, String unit, DataKind kind, double lsb, String monitorAlias) {
            this.name = name;
            this.unit = unit == null ? "" : unit;
            this.kind = kind;
            this.lsb = lsb;
            this.monitorAlias = monitorAlias;
        }

        public int byteLength() {
            switch (kind) {
                case INT32_S:
                case INT32_U:
                case FLOAT32:
                    return 4;
                case FLOAT64:
                    return 8;
                case INT16_S:
                case INT16_U:
                default:
                    return 2;
            }
        }
    }

    private static final Map<Integer, MkV3Type> REGISTRY = new HashMap<>();

    static {
        add(0, "压力表kPa", 1, 4, KIND_PRESSURE, "WMETER", f("压力", "kPa", DataKind.INT16_S, 0.1, "pressure_kpa"));
        add(1, "压力表MPa", 1, 4, KIND_PRESSURE, "WMETER", f("压力", "MPa", DataKind.INT16_S, 0.001, "pressure_mpa"));
        add(2, "压力表BAR", 1, 4, KIND_PRESSURE, "WMETER", f("压力", "Bar", DataKind.INT16_S, 0.01, "pressure"));
        add(3, "压力表PSI", 1, 4, KIND_PRESSURE, "WMETER", f("压力", "PSI", DataKind.INT16_S, 0.1, "pressure"));
        add(4, "压力表Pa", 1, 4, KIND_PRESSURE, "WMETER", f("压力", "Pa", DataKind.INT16_S, 1, "pressure"));
        add(5, "压力表mBAR", 1, 4, KIND_PRESSURE, "WMETER", f("压力", "mBAR", DataKind.INT16_S, 1, "pressure"));
        add(6, "压力表Kgf/cm2", 1, 4, KIND_PRESSURE, "WMETER", f("压力", "Kgf/cm2", DataKind.INT16_S, 0.01, "pressure"));
        add(7, "压力表mmHg", 1, 4, KIND_PRESSURE, "WMETER", f("压力", "mmHg", DataKind.INT16_S, 0.01, "pressure"));
        add(8, "液位表m", 1, 4, KIND_LEVEL, "LLOWT", f("液位", "m", DataKind.INT16_S, 0.01, "level_m"));
        add(9, "液位表cm", 1, 4, KIND_LEVEL, "LLOWT", f("液位", "cm", DataKind.INT16_S, 0.1, "level_cm"));
        add(10, "液位表mm", 1, 4, KIND_LEVEL, "LLOWT", f("液位", "mm", DataKind.INT16_S, 1, "level_mm"));
        add(11, "温度表℃", 1, 4, KIND_TEMP, "WMETER", f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"));
        add(12, "温度表℉", 1, 4, KIND_TEMP, "WMETER", f("温度", "℉", DataKind.INT16_S, 0.1, "temperature"));
        add(13, "湿度表%RH", 1, 4, KIND_HUMIDITY, "WMETER", f("湿度", "%RH", DataKind.INT16_S, 0.1, "humidity"));
        add(14, "电流表", 1, 4, KIND_OTHER, "WMETER", f("电流", "A", DataKind.INT16_S, 0.1, "current"));
        add(15, "电压表", 1, 4, KIND_OTHER, "WMETER", f("电压", "V", DataKind.INT16_S, 0.1, "voltage"));
        add(16, "差压表", 2, 8, KIND_PRESSURE, "WMETER",
                f("静压", "MPa", DataKind.INT16_S, 0.001, "pressure_mpa"),
                f("差压", "kPa", DataKind.INT16_S, 0.1, "pressure_kpa"));
        add(17, "温压表", 2, 8, KIND_PRESSURE, "WMETER",
                f("压力", "MPa", DataKind.INT16_S, 0.001, "pressure_mpa"),
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"));
        add(18, "温湿度表", 2, 8, KIND_HUMIDITY, "WMETER",
                f("湿度", "%RH", DataKind.INT16_S, 0.1, "humidity"),
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"));
        add(19, "振动", 1, 4, KIND_OTHER, "WMETER", f("振动", "mm/s", DataKind.INT16_S, 0.1, "vibration"));
        add(20, "水浸", 1, 4, KIND_WATER, "WWIS", f("模拟量", "", DataKind.INT16_S, 1, "plain"));
        add(21, "差压表进出压", 2, 8, KIND_PRESSURE, "WMETER",
                f("进压", "kPa", DataKind.INT16_S, 1, "pressure_kpa"),
                f("回压", "kPa", DataKind.INT16_S, 1, "pressure_kpa"));
        add(22, "压力表10Pa", 1, 4, KIND_PRESSURE, "WMETER", f("压力", "Pa", DataKind.INT16_S, 10, "pressure_pa"));
        add(23, "倾角", 1, 4, KIND_OTHER, "WMETER", f("倾角", "°", DataKind.INT16_S, 1, "tilt"));
        add(24, "消火栓", 2, 8, KIND_HYDRANT, "OFHAG",
                f("压力", "MPa", DataKind.INT16_S, 0.001, "pressure_mpa"),
                f("倾角", "°", DataKind.INT16_S, 1, "tilt"));
        add(25, "双温度表", 2, 8, KIND_TEMP, "WMETER",
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"));
        add(26, "力KN", 1, 4, KIND_OTHER, "WMETER", f("力", "KN", DataKind.INT16_S, 1, "force_kn"));
        add(27, "流量计", 1, 8, KIND_FLOW, "WMETER",
                f("瞬时流量", "m³/h", DataKind.FLOAT32, 1, "flow"),
                f("瞬时流速", "m/s", DataKind.FLOAT32, 1, "velocity"),
                f("正累积流量", "m³", DataKind.INT32_U, 1, "flow"),
                f("净累积流量", "m³", DataKind.INT32_U, 1, "flow"));
        add(28, "16通道开关量", 1, 4, KIND_SWITCH, "WMETER", f("开关量", "", DataKind.INT16_U, 1, "switch"));
        add(29, "液位超声波探头", 1, 4, KIND_LEVEL, "LLOWT",
                f("液位探头", "m", DataKind.INT16_S, 0.01, "level_m"),
                f("超声波液位", "m", DataKind.INT16_S, 0.01, "level_m"),
                f("换算液位", "m", DataKind.INT16_S, 0.01, "level_m"));
        add(30, "超声波液位表", 1, 4, KIND_LEVEL, "LLOWT", f("液位", "m", DataKind.INT16_S, 0.01, "level_m"));
        add(31, "可燃气体", 1, 4, KIND_GAS, "WCGD", f("可燃气体", "%LEL", DataKind.INT16_U, 0.1, "gas"));
        add(32, "定位坐标", 1, 16, KIND_OTHER, "WMETER",
                f("经度", "°", DataKind.FLOAT64, 1, "longitude"),
                f("纬度", "°", DataKind.FLOAT64, 1, "latitude"));
        add(33, "液位压力表", 2, 8, KIND_LEVEL, "LLOWT",
                f("液位", "m", DataKind.INT16_S, 0.01, "level_m"),
                f("压力", "MPa", DataKind.INT16_S, 0.001, "pressure_mpa"));
        add(34, "消火栓压力", 1, 4, KIND_HYDRANT, "OFHAG", f("压力", "MPa", DataKind.INT16_S, 0.001, "pressure_mpa"));
        add(35, "差压表kPa", 1, 4, KIND_PRESSURE, "WMETER", f("差压", "kPa", DataKind.INT16_S, 0.1, "pressure_kpa"));
        add(36, "消防栓闷盖", 2, 8, KIND_HYDRANT, "OFHAG",
                f("水浸", "", DataKind.INT16_S, 1, "switch"),
                f("闷盖", "", DataKind.INT16_S, 1, "switch"));
        add(37, "井盖状态仪", 1, 4, KIND_WELL, "OFHMC", f("开关量", "", DataKind.INT16_S, 1, "switch"));
        add(38, "液位温度表", 2, 8, KIND_LEVEL, "LLOWT",
                f("液位", "m", DataKind.INT16_S, 0.01, "level_m"),
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"));
        add(39, "温度异动", 2, 8, KIND_TEMP, "WMETER",
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("异动", "", DataKind.INT16_S, 1, "switch"));
        add(40, "井盖液位", 1, 4, KIND_WELL, "OFHMC", f("液位", "m", DataKind.INT16_S, 0.01, "level_m"));
        add(41, "噪声", 1, 4, KIND_OTHER, "WMETER", f("噪声", "dB", DataKind.INT16_S, 1, "noise"));
        add(42, "雷达测距仪", 1, 4, KIND_LEVEL, "LLOWT", f("测距", "m", DataKind.INT16_S, 0.01, "distance_m"));
        add(43, "32通道开关量", 2, 8, KIND_SWITCH, "WMETER", f("开关量", "", DataKind.INT32_U, 1, "switch"));
        add(44, "风速仪", 1, 4, KIND_OTHER, "WMETER",
                f("差压", "Pa", DataKind.INT16_S, 1, "pressure"),
                f("风速", "m/s", DataKind.INT16_S, 0.01, "velocity"),
                f("风量", "m³/m", DataKind.INT16_S, 1, "flow"));
        add(46, "磁致伸缩液位计", 1, 4, KIND_LEVEL, "LLOWT", f("液位", "mm", DataKind.INT16_S, 0.001, "level_m"));
        add(47, "PH检测仪", 1, 4, KIND_OTHER, "WMETER", f("pH", "PH", DataKind.INT16_S, 0.01, "ph"));
        add(48, "电导率检测仪", 1, 4, KIND_OTHER, "WMETER", f("电导率", "uS/cm", DataKind.INT16_S, 0.1, "conductivity"));
        add(49, "液位倾角", 2, 8, KIND_LEVEL, "LLOWT",
                f("液位", "m", DataKind.INT16_S, 0.01, "level_m"),
                f("倾角", "°", DataKind.INT16_S, 1, "tilt"));
        add(50, "4通道压力温度", 1, 4, KIND_PRESSURE, "WMETER",
                f("压力", "MPa", DataKind.INT16_S, 0.001, "pressure_mpa"),
                f("压力", "MPa", DataKind.INT16_S, 0.001, "pressure_mpa"),
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("流量", "m³/h", DataKind.INT16_S, 0.01, "flow"));
        add(51, "ORP检测仪", 1, 4, KIND_OTHER, "WMETER", f("ORP", "mV", DataKind.INT16_S, 1, "orp"));
        add(52, "力KN-32位", 1, 8, KIND_OTHER, "WMETER", f("力", "KN", DataKind.INT32_S, 1, "force_kn"));
        add(53, "流量计m3", 1, 8, KIND_FLOW, "WMETER", f("累积流量", "m³", DataKind.INT32_U, 0.01, "flow"));
        add(54, "氨氮检测仪", 1, 4, KIND_OTHER, "WMETER",
                f("氨氮", "mg/L", DataKind.INT16_S, 0.1, "ammonia"),
                f("pH", "pH", DataKind.INT16_S, 0.1, "ph"),
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"));
        add(55, "COD检测仪", 1, 4, KIND_OTHER, "WMETER",
                f("COD", "mg/L", DataKind.INT16_S, 0.1, "cod"),
                f("浊度", "NTU", DataKind.INT16_S, 0.1, "turbidity"),
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"));
        add(56, "溶解氧检测仪", 1, 4, KIND_OTHER, "WMETER",
                f("溶解氧", "mg/L", DataKind.INT16_S, 0.01, "dissolved-oxygen"),
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("溶解氧单位", "", DataKind.INT16_S, 1, "switch"));
        add(57, "浊度检测仪", 1, 4, KIND_OTHER, "WMETER", f("浊度", "NTU", DataKind.INT16_S, 0.1, "turbidity"));
        add(58, "消火栓流量", 1, 4, KIND_HYDRANT, "OFHAG",
                f("压力", "MPa", DataKind.INT16_S, 0.001, "pressure_mpa"),
                f("倾角", "°", DataKind.INT16_S, 1, "tilt"),
                f("流量", "L/s", DataKind.INT16_S, 0.01, "flow_ls"),
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"));
        add(59, "门磁开关", 1, 4, KIND_SWITCH, "WMETER", f("红外计数", "", DataKind.INT16_U, 1, "switch"));
        add(60, "电表", 1, 4, KIND_OTHER, "WMETER",
                f("有功总电量", "kWh", DataKind.INT32_U, 0.01, "energy"),
                f("A相电压", "V", DataKind.INT16_U, 0.1, "voltage"),
                f("B相电压", "V", DataKind.INT16_U, 0.1, "voltage"),
                f("C相电压", "V", DataKind.INT16_U, 0.1, "voltage"),
                f("A相电流", "A", DataKind.INT16_U, 0.01, "current"),
                f("B相电流", "A", DataKind.INT16_U, 0.01, "current"),
                f("C相电流", "A", DataKind.INT16_U, 0.01, "current"),
                f("总有功功率", "kW", DataKind.INT16_U, 0.001, "power_kw"),
                f("总功率因数", "", DataKind.INT16_U, 0.001, "power-factor"));
        add(61, "压力0.01MPa", 1, 4, KIND_PRESSURE, "WMETER", f("压力", "MPa", DataKind.INT16_U, 0.01, "pressure_mpa"));
        add(62, "振弦温度", 2, 16, KIND_OTHER, "WMETER",
                f("振弦", "", DataKind.INT32_S, 0.01, "frequency_hz"),
                f("温度", "℃", DataKind.INT32_S, 0.1, "temperature"));
        add(63, "液位0.001m", 2, 8, KIND_LEVEL, "LLOWT", f("液位", "m", DataKind.INT16_U, 0.001, "level_m"));
        add(64, "蓝绿藻", 2, 16, KIND_OTHER, "WMETER",
                f("蓝绿藻", "cell/mL", DataKind.INT32_S, 1, "algae"),
                f("温度", "℃", DataKind.INT32_S, 0.1, "temperature"));
        add(65, "三通道温度", 1, 4, KIND_TEMP, "WMETER",
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"));
        add(66, "双通道压力", 2, 8, KIND_PRESSURE, "WMETER",
                f("压力", "MPa", DataKind.INT16_S, 0.001, "pressure_mpa"),
                f("压力", "kPa", DataKind.INT16_S, 0.01, "pressure_kpa"));
        add(67, "智能井盖监测终端", 2, 8, KIND_WELL, "OFHMC",
                f("水浸", "", DataKind.INT16_S, 1, "plain"),
                f("倾角", "°", DataKind.INT16_S, 1, "tilt"));
        add(68, "PH监测终端", 2, 8, KIND_OTHER, "WMETER",
                f("pH", "pH", DataKind.INT16_S, 0.01, "ph"),
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"));
        add(69, "电导率监测终端", 2, 16, KIND_OTHER, "WMETER",
                f("电导率", "uS/cm", DataKind.INT32_S, 0.1, "conductivity"),
                f("温度", "℃", DataKind.INT32_S, 0.1, "temperature"));
        add(70, "拉线位移", 1, 8, KIND_OTHER, "WMETER", f("位移", "mm", DataKind.INT32_S, 0.01, "distance_mm"));
        add(71, "水质分析仪", 1, 4, KIND_OTHER, "WMETER",
                f("氨氮", "mg/L", DataKind.INT16_S, 0.1, "concentration"),
                f("硝氮", "mg/L", DataKind.INT16_S, 0.1, "concentration"),
                f("氯离子", "mg/L", DataKind.INT16_S, 0.1, "concentration"),
                f("pH", "pH", DataKind.INT16_S, 0.01, "ph"),
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"));
        add(72, "智能空气质量", 1, 4, KIND_OTHER, "WMETER",
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("湿度", "%RH", DataKind.INT16_S, 0.1, "humidity"),
                f("PM1.0", "ug/m³", DataKind.INT16_S, 1, "concentration_ugm3"),
                f("PM2.5", "ug/m³", DataKind.INT16_S, 1, "concentration_ugm3"),
                f("PM10", "ug/m³", DataKind.INT16_S, 1, "concentration_ugm3"),
                f("CO2", "ppm", DataKind.INT16_S, 1, "co2"),
                f("甲醛", "mg/m³", DataKind.INT16_S, 0.001, "concentration_mgm3"),
                f("TVOC", "mg/m³", DataKind.INT16_S, 0.01, "concentration_mgm3"));
        add(73, "燃气流量表", 1, 8, KIND_FLOW, "WMETER",
                f("标况流量", "m³/H", DataKind.FLOAT32, 1, "flow"),
                f("工况流量", "m³/H", DataKind.FLOAT32, 1, "flow"),
                f("标况累计流量", "m³", DataKind.FLOAT64, 1, "flow"),
                f("工况累计流量", "m³", DataKind.FLOAT64, 1, "flow"),
                f("温度", "℃", DataKind.FLOAT32, 1, "temperature"),
                f("压力", "kPa", DataKind.FLOAT32, 1, "pressure"),
                f("状态", "", DataKind.INT16_S, 1, "switch"));
        add(74, "多普勒流速仪", 1, 4, KIND_FLOW, "WMETER",
                f("流速", "m/s", DataKind.INT16_S, 0.001, "velocity"),
                f("液位", "m", DataKind.INT16_S, 0.001, "level_m"),
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"));
        add(75, "光照度", 1, 4, KIND_OTHER, "WMETER", f("光照度", "lux", DataKind.INT16_S, 1, "illuminance"));
        add(76, "光照度无符号", 1, 4, KIND_OTHER, "WMETER", f("光照度", "lux", DataKind.INT16_U, 1, "illuminance"));
        add(77, "乙醇", 1, 4, KIND_GAS, "WCGD", f("乙醇", "ppm", DataKind.INT16_S, 1, "gas"));
        add(78, "空气质量O2", 1, 4, KIND_OTHER, "WMETER",
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("湿度", "%RH", DataKind.INT16_S, 0.1, "humidity"),
                f("PM1.0", "ug/m³", DataKind.INT16_S, 1, "pm1"),
                f("PM2.5", "ug/m³", DataKind.INT16_S, 1, "pm25"),
                f("PM10", "ug/m³", DataKind.INT16_S, 1, "pm10"),
                f("CO2", "ppm", DataKind.INT16_S, 1, "co2"),
                f("氧气", "%", DataKind.INT16_S, 0.1, "o2"),
                f("TVOC", "mg/m³", DataKind.INT16_S, 0.01, "tvoc"));
        add(79, "空气质量S", 1, 4, KIND_OTHER, "WMETER",
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("湿度", "%RH", DataKind.INT16_S, 0.1, "humidity"),
                f("PM1.0", "ug/m³", DataKind.INT16_S, 1, "pm1"),
                f("PM2.5", "ug/m³", DataKind.INT16_S, 1, "pm25"),
                f("PM10", "ug/m³", DataKind.INT16_S, 1, "pm10"),
                f("硫化氢", "ppm", DataKind.INT16_S, 0.01, "h2s"),
                f("氨气", "ppm", DataKind.INT16_S, 0.1, "nh3"),
                f("二氧化硫", "ppm", DataKind.INT16_S, 0.01, "so2"));
        add(80, "温度0.01℃", 1, 4, KIND_TEMP, "WMETER", f("温度", "℃", DataKind.INT16_S, 0.01, "temperature"));
        add(81, "气体探测器", 1, 4, KIND_GAS, "WCGD", f("气体浓度", "umol/mol", DataKind.INT16_S, 1, "gas"));
        add(83, "土壤温湿度", 1, 4, KIND_OTHER, "WMETER",
                f("湿度", "%RH", DataKind.INT16_S, 0.1, "humidity"),
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("电导率", "uS/cm", DataKind.INT16_S, 1, "conductivity"));
        add(84, "雷达液位计", 1, 8, KIND_LEVEL, "LLOWT", f("液位", "m", DataKind.INT32_S, 0.001, "level_m"));
        add(86, "振弦温度Hz", 2, 16, KIND_OTHER, "WMETER",
                f("振弦频率", "Hz", DataKind.INT32_S, 0.1, "frequency"),
                f("温度", "℃", DataKind.INT32_S, 0.1, "temperature"));
        add(87, "余氯检测仪", 2, 8, KIND_OTHER, "WMETER",
                f("余氯", "mg/L", DataKind.INT16_S, 0.01, "chlorine"),
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"));
        add(88, "3压力1温度", 1, 4, KIND_PRESSURE, "WMETER",
                f("压力", "MPa", DataKind.INT16_S, 0.001, "pressure"),
                f("压力", "MPa", DataKind.INT16_S, 0.001, "pressure"),
                f("压力", "MPa", DataKind.INT16_S, 0.001, "pressure"),
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("流量", "m³/h", DataKind.INT16_S, 0.01, "flow"));
        add(89, "流量定位", 1, 8, KIND_FLOW, "WMETER",
                f("瞬时流量", "m³/h", DataKind.FLOAT32, 1, "flow"),
                f("瞬时流速", "m/s", DataKind.FLOAT32, 1, "velocity"),
                f("正累积流量", "m³", DataKind.INT32_S, 1, "flow"),
                f("净累积流量", "m³", DataKind.INT32_S, 1, "flow"),
                f("日累积流量", "m³", DataKind.INT32_S, 0.1, "flow"),
                f("经度", "°", DataKind.FLOAT64, 1, "longitude"),
                f("纬度", "°", DataKind.FLOAT64, 1, "latitude"));
        add(90, "液位双路报警器", 2, 8, KIND_LEVEL, "LLOWT",
                f("液位", "m", DataKind.INT16_S, 0.01, "level_m"),
                f("液位", "m", DataKind.INT16_S, 0.01, "level_m"));
        add(91, "八通道温度", 1, 4, KIND_TEMP, "WMETER",
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"));
        add(92, "四通道温度", 1, 4, KIND_TEMP, "WMETER",
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"));
        add(93, "液位温湿度", 1, 4, KIND_LEVEL, "LLOWT",
                f("液位", "m", DataKind.INT16_S, 0.01, "level_m"),
                f("湿度", "%RH", DataKind.INT16_S, 0.1, "humidity"),
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"));
        add(96, "空气质量CO/SO2", 1, 4, KIND_OTHER, "WMETER",
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("湿度", "%RH", DataKind.INT16_S, 0.1, "humidity"),
                f("PM1.0", "ug/m³", DataKind.INT16_S, 1, "pm1"),
                f("PM2.5", "ug/m³", DataKind.INT16_S, 1, "pm25"),
                f("PM10", "ug/m³", DataKind.INT16_S, 1, "pm10"),
                f("CO2", "ppm", DataKind.INT16_S, 1, "co2"),
                f("一氧化碳", "ppm", DataKind.INT16_S, 1, "co"),
                f("二氧化硫", "ppm", DataKind.INT16_S, 0.01, "so2"));
        add(99, "可燃有毒气体", 1, 4, KIND_GAS, "WCGD",
                f("气体类型", "", DataKind.INT16_S, 1, "gas-type"),
                f("传感器状态", "", DataKind.INT16_S, 1, "switch"),
                f("浓度", "", DataKind.INT16_S, 1, "gas"));
        add(100, "开关模拟继电器采集", 1, 4, KIND_SWITCH, "WMETER",
                f("开关量", "", DataKind.INT16_U, 1, "switch"),
                f("电流", "mA", DataKind.INT16_U, 0.01, "current"),
                f("电流", "mA", DataKind.INT16_U, 0.01, "current"),
                f("电流", "mA", DataKind.INT16_U, 0.01, "current"),
                f("电流", "mA", DataKind.INT16_U, 0.01, "current"),
                f("电流", "mA", DataKind.INT16_U, 0.01, "current"),
                f("电流", "mA", DataKind.INT16_U, 0.01, "current"),
                f("电流", "mA", DataKind.INT16_U, 0.01, "current"),
                f("电流", "mA", DataKind.INT16_U, 0.01, "current"),
                f("继电器", "", DataKind.INT16_U, 1, "switch"));
        add(101, "控制器8DI4DO", 1, 4, KIND_SWITCH, "WMETER",
                f("通道个数", "", DataKind.INT16_S, 1, "switch"),
                f("采集通道", "", DataKind.INT16_U, 1, "switch"),
                f("输出通道", "", DataKind.INT16_U, 1, "switch"));
        add(102, "声光报警器", 1, 4, KIND_OTHER, "WMETER", f("报警状态", "", DataKind.INT16_S, 1, "switch"));
        add(105, "空气质量CO/NH3", 1, 4, KIND_OTHER, "WMETER",
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("湿度", "%RH", DataKind.INT16_S, 0.1, "humidity"),
                f("PM1.0", "ug/m³", DataKind.INT16_S, 1, "pm1"),
                f("PM2.5", "ug/m³", DataKind.INT16_S, 1, "pm25"),
                f("PM10", "ug/m³", DataKind.INT16_S, 1, "pm10"),
                f("CO2", "ppm", DataKind.INT16_S, 1, "co2"),
                f("一氧化碳", "ppm", DataKind.INT16_S, 1, "co"),
                f("氨气", "ppm", DataKind.INT16_S, 0.1, "nh3"));
        add(106, "温度振动传感器", 1, 4, KIND_OTHER, "WMETER",
                f("温度", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("X轴振速", "mm/s", DataKind.INT16_S, 0.1, "vibration"),
                f("Y轴振速", "mm/s", DataKind.INT16_S, 0.1, "vibration"),
                f("Z轴振速", "mm/s", DataKind.INT16_S, 0.1, "vibration"),
                f("X轴加速度", "m/s²", DataKind.INT16_S, 0.1, "vibration"),
                f("Y轴加速度", "m/s²", DataKind.INT16_S, 0.1, "vibration"),
                f("Z轴加速度", "m/s²", DataKind.INT16_S, 0.1, "vibration"),
                f("X轴位移", "um", DataKind.INT16_S, 1, "displacement"),
                f("Y轴位移", "um", DataKind.INT16_S, 1, "displacement"),
                f("Z轴位移", "um", DataKind.INT16_S, 1, "displacement"),
                f("振动报警", "", DataKind.INT16_S, 1, "switch"));
    }

    private final int code;
    private final String name;
    private final int alarmGroups;
    private final int dataBytes;
    private final int alarmBytes;
    private final double lsb;
    private final String unit;
    private final String kind;
    private final String typeCode;
    private final List<Field> fields;

    private MkV3Type(int code, String name, int alarmGroups, int dataBytes, int alarmBytes,
                     double lsb, String unit, String kind, String typeCode, List<Field> fields) {
        this.code = code;
        this.name = name;
        this.alarmGroups = alarmGroups;
        this.dataBytes = dataBytes;
        this.alarmBytes = alarmBytes;
        this.lsb = lsb;
        this.unit = unit;
        this.kind = kind;
        this.typeCode = typeCode;
        this.fields = fields;
    }

    public static MkV3Type of(int code) {
        return REGISTRY.get(code);
    }

    public Field fieldAt(int index) {
        if (fields == null || fields.isEmpty()) {
            return null;
        }
        return index < fields.size() ? fields.get(index) : fields.get(0);
    }

    public static Field f(String name, String unit, DataKind kind, double lsb, String monitorAlias) {
        return new Field(name, unit, kind, lsb, monitorAlias);
    }

    private static void add(int code, String name, int alarmGroups, int alarmBytes,
                            String kind, String typeCode, Field... fieldArr) {
        List<Field> fields = new ArrayList<>();
        if (fieldArr != null) {
            Collections.addAll(fields, fieldArr);
        }
        int dataBytes = 0;
        for (Field field : fields) {
            dataBytes += field.byteLength();
        }
        Field first = fields.isEmpty() ? null : fields.get(0);
        REGISTRY.put(code, new MkV3Type(
                code, name, alarmGroups, dataBytes, alarmBytes,
                first == null ? 1 : first.getLsb(),
                first == null ? "" : first.getUnit(),
                kind, typeCode, Collections.unmodifiableList(fields)
        ));
    }
}
