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
        add(0, "压力表kPa", 1, 4, KIND_PRESSURE, "WMETER", f("pressure", "kPa", DataKind.INT16_S, 0.1, "pressure"));
        add(1, "压力表MPa", 1, 4, KIND_PRESSURE, "WMETER", f("pressure", "MPa", DataKind.INT16_S, 0.001, "pressure"));
        add(2, "压力表BAR", 1, 4, KIND_PRESSURE, "WMETER", f("pressure", "Bar", DataKind.INT16_S, 0.01, "pressure"));
        add(3, "压力表PSI", 1, 4, KIND_PRESSURE, "WMETER", f("pressure", "PSI", DataKind.INT16_S, 0.1, "pressure"));
        add(4, "压力表Pa", 1, 4, KIND_PRESSURE, "WMETER", f("pressure", "Pa", DataKind.INT16_S, 1, "pressure"));
        add(5, "压力表mBAR", 1, 4, KIND_PRESSURE, "WMETER", f("pressure", "mBAR", DataKind.INT16_S, 1, "pressure"));
        add(6, "压力表Kgf/cm2", 1, 4, KIND_PRESSURE, "WMETER", f("pressure", "Kgf/cm2", DataKind.INT16_S, 0.01, "pressure"));
        add(7, "压力表mmHg", 1, 4, KIND_PRESSURE, "WMETER", f("pressure", "mmHg", DataKind.INT16_S, 0.01, "pressure"));
        add(8, "液位表m", 1, 4, KIND_LEVEL, "LLOWT", f("level", "m", DataKind.INT16_S, 0.01, "liquid-level"));
        add(9, "液位表cm", 1, 4, KIND_LEVEL, "LLOWT", f("level", "cm", DataKind.INT16_S, 0.1, "liquid-level"));
        add(10, "液位表mm", 1, 4, KIND_LEVEL, "LLOWT", f("level", "mm", DataKind.INT16_S, 1, "liquid-level"));
        add(11, "温度表℃", 1, 4, KIND_TEMP, "WMETER", f("temperature", "℃", DataKind.INT16_S, 0.1, "temperature"));
        add(12, "温度表℉", 1, 4, KIND_TEMP, "WMETER", f("temperature", "℉", DataKind.INT16_S, 0.1, "temperature"));
        add(13, "湿度表%RH", 1, 4, KIND_HUMIDITY, "WMETER", f("humidity", "%RH", DataKind.INT16_S, 0.1, "humidity"));
        add(14, "电流表", 1, 4, KIND_OTHER, "WMETER", f("current", "A", DataKind.INT16_S, 0.1, "current"));
        add(15, "电压表", 1, 4, KIND_OTHER, "WMETER", f("voltage", "V", DataKind.INT16_S, 0.1, "voltage"));
        add(16, "差压表", 2, 8, KIND_PRESSURE, "WMETER",
                f("staticPressure", "MPa", DataKind.INT16_S, 0.001, "pressure"),
                f("diffPressure", "kPa", DataKind.INT16_S, 0.1, "pressure"));
        add(17, "温压表", 2, 8, KIND_PRESSURE, "WMETER",
                f("pressure", "MPa", DataKind.INT16_S, 0.001, "pressure"),
                f("temperature", "℃", DataKind.INT16_S, 0.1, "temperature"));
        add(18, "温湿度表", 2, 8, KIND_HUMIDITY, "WMETER",
                f("humidity", "%RH", DataKind.INT16_S, 0.1, "humidity"),
                f("temperature", "℃", DataKind.INT16_S, 0.1, "temperature"));
        add(19, "振动", 1, 4, KIND_OTHER, "WMETER", f("vibration", "mm/s", DataKind.INT16_S, 0.1, "vibration"));
        add(20, "水浸", 1, 4, KIND_WATER, "WWIS", f("waterImmersion", "", DataKind.INT16_S, 1, "switch"));
        add(21, "差压表进出压", 2, 8, KIND_PRESSURE, "WMETER",
                f("inPressure", "kPa", DataKind.INT16_S, 1, "pressure"),
                f("returnPressure", "kPa", DataKind.INT16_S, 1, "pressure"));
        add(22, "压力表10Pa", 1, 4, KIND_PRESSURE, "WMETER", f("pressure", "Pa", DataKind.INT16_S, 10, "pressure"));
        add(23, "倾角", 1, 4, KIND_OTHER, "WMETER", f("tilt", "°", DataKind.INT16_S, 1, "tilt"));
        add(24, "消火栓", 2, 8, KIND_HYDRANT, "OFHAG",
                f("pressure", "MPa", DataKind.INT16_S, 0.001, "pressure"),
                f("tilt", "°", DataKind.INT16_S, 1, "tilt"));
        add(25, "双温度表", 2, 8, KIND_TEMP, "WMETER",
                f("temperature1", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("temperature2", "℃", DataKind.INT16_S, 0.1, "temperature"));
        add(26, "力KN", 1, 4, KIND_OTHER, "WMETER", f("force", "KN", DataKind.INT16_S, 1, "force"));
        add(27, "流量计", 1, 8, KIND_FLOW, "WMETER",
                f("instantFlow", "m³/h", DataKind.FLOAT32, 1, "flow"),
                f("instantVelocity", "m/s", DataKind.FLOAT32, 1, "velocity"),
                f("positiveTotal", "m³", DataKind.INT32_U, 1, "flow"),
                f("netTotal", "m³", DataKind.INT32_U, 1, "flow"));
        add(28, "16通道开关量", 1, 4, KIND_SWITCH, "WMETER", f("diBits", "", DataKind.INT16_U, 1, "switch"));
        add(29, "液位超声波探头", 1, 4, KIND_LEVEL, "LLOWT",
                f("probeLevel", "m", DataKind.INT16_S, 0.01, "liquid-level"),
                f("ultrasonicLevel", "m", DataKind.INT16_S, 0.01, "liquid-level"),
                f("convertedLevel", "m", DataKind.INT16_S, 0.01, "liquid-level"));
        add(30, "超声波液位表", 1, 4, KIND_LEVEL, "LLOWT", f("level", "m", DataKind.INT16_S, 0.01, "liquid-level"));
        add(31, "可燃气体", 1, 4, KIND_GAS, "WCGD", f("gas", "%LEL", DataKind.INT16_U, 0.1, "gas"));
        add(32, "定位坐标", 1, 16, KIND_OTHER, "WMETER",
                f("longitude", "°", DataKind.FLOAT64, 1, "longitude"),
                f("latitude", "°", DataKind.FLOAT64, 1, "latitude"));
        add(33, "液位压力表", 2, 8, KIND_LEVEL, "LLOWT",
                f("level", "m", DataKind.INT16_S, 0.01, "liquid-level"),
                f("pressure", "MPa", DataKind.INT16_S, 0.001, "pressure"));
        add(34, "消火栓压力", 1, 4, KIND_HYDRANT, "OFHAG", f("pressure", "MPa", DataKind.INT16_S, 0.001, "pressure"));
        add(35, "差压表kPa", 1, 4, KIND_PRESSURE, "WMETER", f("diffPressure", "kPa", DataKind.INT16_S, 0.1, "pressure"));
        add(36, "消防栓闷盖", 2, 8, KIND_HYDRANT, "OFHAG",
                f("waterAd", "", DataKind.INT16_S, 1, "switch"),
                f("coverAd", "", DataKind.INT16_S, 1, "switch"));
        add(37, "井盖状态仪", 1, 4, KIND_WELL, "OFHMC", f("switchAd", "", DataKind.INT16_S, 1, "switch"));
        add(38, "液位温度表", 2, 8, KIND_LEVEL, "LLOWT",
                f("level", "m", DataKind.INT16_S, 0.01, "liquid-level"),
                f("temperature", "℃", DataKind.INT16_S, 0.1, "temperature"));
        add(39, "温度异动", 2, 8, KIND_TEMP, "WMETER",
                f("temperature", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("switchAd", "", DataKind.INT16_S, 1, "switch"));
        add(40, "井盖液位", 1, 4, KIND_WELL, "OFHMC", f("level", "m", DataKind.INT16_S, 0.01, "liquid-level"));
        add(41, "噪声", 1, 4, KIND_OTHER, "WMETER", f("noise", "dB", DataKind.INT16_S, 1, "noise"));
        add(42, "雷达测距仪", 1, 4, KIND_LEVEL, "LLOWT", f("distance", "m", DataKind.INT16_S, 0.01, "liquid-level"));
        add(43, "32通道开关量", 2, 8, KIND_SWITCH, "WMETER", f("diBits", "", DataKind.INT32_U, 1, "switch"));
        add(44, "风速仪", 1, 4, KIND_OTHER, "WMETER",
                f("diffPressure", "Pa", DataKind.INT16_S, 1, "pressure"),
                f("windSpeed", "m/s", DataKind.INT16_S, 0.01, "velocity"),
                f("airVolume", "m³/m", DataKind.INT16_S, 1, "flow"));
        add(46, "磁致伸缩液位计", 1, 4, KIND_LEVEL, "LLOWT", f("level", "mm", DataKind.INT16_S, 0.001, "liquid-level"));
        add(47, "PH检测仪", 1, 4, KIND_OTHER, "WMETER", f("ph", "PH", DataKind.INT16_S, 0.01, "ph"));
        add(48, "电导率检测仪", 1, 4, KIND_OTHER, "WMETER", f("conductivity", "uS/cm", DataKind.INT16_S, 0.1, "conductivity"));
        add(49, "液位倾角", 2, 8, KIND_LEVEL, "LLOWT",
                f("level", "m", DataKind.INT16_S, 0.01, "liquid-level"),
                f("tilt", "°", DataKind.INT16_S, 1, "tilt"));
        add(50, "4通道压力温度", 1, 4, KIND_PRESSURE, "WMETER",
                f("pressure1", "MPa", DataKind.INT16_S, 0.001, "pressure"),
                f("pressure2", "MPa", DataKind.INT16_S, 0.001, "pressure"),
                f("temperature1", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("temperature2", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("flow", "m³/h", DataKind.INT16_S, 0.01, "flow"));
        add(51, "ORP检测仪", 1, 4, KIND_OTHER, "WMETER", f("orp", "mV", DataKind.INT16_S, 1, "orp"));
        add(52, "力KN-32位", 1, 8, KIND_OTHER, "WMETER", f("force", "KN", DataKind.INT32_S, 1, "force"));
        add(53, "流量计m3", 1, 8, KIND_FLOW, "WMETER", f("totalFlow", "m³", DataKind.INT32_U, 0.01, "flow"));
        add(54, "氨氮检测仪", 1, 4, KIND_OTHER, "WMETER",
                f("ammonia", "mg/L", DataKind.INT16_S, 0.1, "ammonia"),
                f("ph", "pH", DataKind.INT16_S, 0.1, "ph"),
                f("temperature", "℃", DataKind.INT16_S, 0.1, "temperature"));
        add(55, "COD检测仪", 1, 4, KIND_OTHER, "WMETER",
                f("cod", "mg/L", DataKind.INT16_S, 0.1, "cod"),
                f("turbidity", "NTU", DataKind.INT16_S, 0.1, "turbidity"),
                f("temperature", "℃", DataKind.INT16_S, 0.1, "temperature"));
        add(56, "溶解氧检测仪", 1, 4, KIND_OTHER, "WMETER",
                f("dissolvedOxygen", "mg/L", DataKind.INT16_S, 0.01, "dissolved-oxygen"),
                f("temperature", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("doUnit", "", DataKind.INT16_S, 1, "switch"));
        add(57, "浊度检测仪", 1, 4, KIND_OTHER, "WMETER", f("turbidity", "NTU", DataKind.INT16_S, 0.1, "turbidity"));
        add(58, "消火栓流量", 1, 4, KIND_HYDRANT, "OFHAG",
                f("pressure", "MPa", DataKind.INT16_S, 0.001, "pressure"),
                f("tilt", "°", DataKind.INT16_S, 1, "tilt"),
                f("flow", "L/s", DataKind.INT16_S, 0.01, "flow"),
                f("temperature", "℃", DataKind.INT16_S, 0.1, "temperature"));
        add(59, "门磁开关", 1, 4, KIND_SWITCH, "WMETER", f("infraredCount", "", DataKind.INT16_U, 1, "switch"));
        add(60, "电表", 1, 4, KIND_OTHER, "WMETER",
                f("activeEnergy", "kWh", DataKind.INT32_U, 0.01, "energy"),
                f("voltageA", "V", DataKind.INT16_U, 0.1, "voltage"),
                f("voltageB", "V", DataKind.INT16_U, 0.1, "voltage"),
                f("voltageC", "V", DataKind.INT16_U, 0.1, "voltage"),
                f("currentA", "A", DataKind.INT16_U, 0.01, "current"),
                f("currentB", "A", DataKind.INT16_U, 0.01, "current"),
                f("currentC", "A", DataKind.INT16_U, 0.01, "current"),
                f("activePower", "kW", DataKind.INT16_U, 0.001, "power"),
                f("powerFactor", "", DataKind.INT16_U, 0.001, "power-factor"));
        add(61, "压力0.01MPa", 1, 4, KIND_PRESSURE, "WMETER", f("pressure", "MPa", DataKind.INT16_U, 0.01, "pressure"));
        add(62, "振弦温度", 2, 16, KIND_OTHER, "WMETER",
                f("vibratingWire", "", DataKind.INT32_S, 0.01, "frequency"),
                f("temperature", "℃", DataKind.INT32_S, 0.1, "temperature"));
        add(63, "液位0.001m", 2, 8, KIND_LEVEL, "LLOWT", f("level", "m", DataKind.INT16_U, 0.001, "liquid-level"));
        add(64, "蓝绿藻", 2, 16, KIND_OTHER, "WMETER",
                f("algae", "cell/mL", DataKind.INT32_S, 1, "algae"),
                f("temperature", "℃", DataKind.INT32_S, 0.1, "temperature"));
        add(65, "三通道温度", 1, 4, KIND_TEMP, "WMETER",
                f("temperature1", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("temperature2", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("temperature3", "℃", DataKind.INT16_S, 0.1, "temperature"));
        add(66, "双通道压力", 2, 8, KIND_PRESSURE, "WMETER",
                f("pressure1", "MPa", DataKind.INT16_S, 0.001, "pressure"),
                f("pressure2", "kPa", DataKind.INT16_S, 0.01, "pressure"));
        add(67, "智能井盖监测终端", 2, 8, KIND_WELL, "OFHMC",
                f("waterImmersion", "", DataKind.INT16_S, 1, "switch"),
                f("tilt", "°", DataKind.INT16_S, 1, "tilt"));
        add(68, "PH监测终端", 2, 8, KIND_OTHER, "WMETER",
                f("ph", "pH", DataKind.INT16_S, 0.01, "ph"),
                f("temperature", "℃", DataKind.INT16_S, 0.1, "temperature"));
        add(69, "电导率监测终端", 2, 16, KIND_OTHER, "WMETER",
                f("conductivity", "uS/cm", DataKind.INT32_S, 0.1, "conductivity"),
                f("temperature", "℃", DataKind.INT32_S, 0.1, "temperature"));
        add(70, "拉线位移", 1, 8, KIND_OTHER, "WMETER", f("displacement", "mm", DataKind.INT32_S, 0.01, "displacement"));
        add(71, "水质分析仪", 1, 4, KIND_OTHER, "WMETER",
                f("ammonia", "mg/L", DataKind.INT16_S, 0.1, "ammonia"),
                f("nitrate", "mg/L", DataKind.INT16_S, 0.1, "nitrate"),
                f("chloride", "mg/L", DataKind.INT16_S, 0.1, "chloride"),
                f("ph", "pH", DataKind.INT16_S, 0.01, "ph"),
                f("temperature", "℃", DataKind.INT16_S, 0.1, "temperature"));
        add(72, "智能空气质量", 1, 4, KIND_OTHER, "WMETER",
                f("temperature", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("humidity", "%RH", DataKind.INT16_S, 0.1, "humidity"),
                f("pm1", "ug/m³", DataKind.INT16_S, 1, "pm1"),
                f("pm25", "ug/m³", DataKind.INT16_S, 1, "pm25"),
                f("pm10", "ug/m³", DataKind.INT16_S, 1, "pm10"),
                f("co2", "ppm", DataKind.INT16_S, 1, "co2"),
                f("ch2o", "mg/m³", DataKind.INT16_S, 0.001, "ch2o"),
                f("tvoc", "mg/m³", DataKind.INT16_S, 0.01, "tvoc"));
        add(73, "燃气流量表", 1, 8, KIND_FLOW, "WMETER",
                f("standardFlow", "m³/H", DataKind.FLOAT32, 1, "flow"),
                f("workingFlow", "m³/H", DataKind.FLOAT32, 1, "flow"),
                f("standardTotal", "m³", DataKind.FLOAT64, 1, "flow"),
                f("workingTotal", "m³", DataKind.FLOAT64, 1, "flow"),
                f("temperature", "℃", DataKind.FLOAT32, 1, "temperature"),
                f("pressure", "kPa", DataKind.FLOAT32, 1, "pressure"),
                f("status", "", DataKind.INT16_S, 1, "switch"));
        add(74, "多普勒流速仪", 1, 4, KIND_FLOW, "WMETER",
                f("velocity", "m/s", DataKind.INT16_S, 0.001, "velocity"),
                f("level", "m", DataKind.INT16_S, 0.001, "liquid-level"),
                f("temperature", "℃", DataKind.INT16_S, 0.1, "temperature"));
        add(75, "光照度", 1, 4, KIND_OTHER, "WMETER", f("illuminance", "lux", DataKind.INT16_S, 1, "illuminance"));
        add(76, "光照度无符号", 1, 4, KIND_OTHER, "WMETER", f("illuminance", "lux", DataKind.INT16_U, 1, "illuminance"));
        add(77, "乙醇", 1, 4, KIND_GAS, "WCGD", f("ethanol", "ppm", DataKind.INT16_S, 1, "gas"));
        add(78, "空气质量O2", 1, 4, KIND_OTHER, "WMETER",
                f("temperature", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("humidity", "%RH", DataKind.INT16_S, 0.1, "humidity"),
                f("pm1", "ug/m³", DataKind.INT16_S, 1, "pm1"),
                f("pm25", "ug/m³", DataKind.INT16_S, 1, "pm25"),
                f("pm10", "ug/m³", DataKind.INT16_S, 1, "pm10"),
                f("co2", "ppm", DataKind.INT16_S, 1, "co2"),
                f("o2", "%", DataKind.INT16_S, 0.1, "o2"),
                f("tvoc", "mg/m³", DataKind.INT16_S, 0.01, "tvoc"));
        add(79, "空气质量S", 1, 4, KIND_OTHER, "WMETER",
                f("temperature", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("humidity", "%RH", DataKind.INT16_S, 0.1, "humidity"),
                f("pm1", "ug/m³", DataKind.INT16_S, 1, "pm1"),
                f("pm25", "ug/m³", DataKind.INT16_S, 1, "pm25"),
                f("pm10", "ug/m³", DataKind.INT16_S, 1, "pm10"),
                f("h2s", "ppm", DataKind.INT16_S, 0.01, "h2s"),
                f("nh3", "ppm", DataKind.INT16_S, 0.1, "nh3"),
                f("so2", "ppm", DataKind.INT16_S, 0.01, "so2"));
        add(80, "温度0.01℃", 1, 4, KIND_TEMP, "WMETER", f("temperature", "℃", DataKind.INT16_S, 0.01, "temperature"));
        add(81, "气体探测器", 1, 4, KIND_GAS, "WCGD", f("gas", "umol/mol", DataKind.INT16_S, 1, "gas"));
        add(83, "土壤温湿度", 1, 4, KIND_OTHER, "WMETER",
                f("humidity", "%RH", DataKind.INT16_S, 0.1, "humidity"),
                f("temperature", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("conductivity", "uS/cm", DataKind.INT16_S, 1, "conductivity"));
        add(84, "雷达液位计", 1, 8, KIND_LEVEL, "LLOWT", f("level", "m", DataKind.INT32_S, 0.001, "liquid-level"));
        add(86, "振弦温度Hz", 2, 16, KIND_OTHER, "WMETER",
                f("frequency", "Hz", DataKind.INT32_S, 0.1, "frequency"),
                f("temperature", "℃", DataKind.INT32_S, 0.1, "temperature"));
        add(87, "余氯检测仪", 2, 8, KIND_OTHER, "WMETER",
                f("residualChlorine", "mg/L", DataKind.INT16_S, 0.01, "chlorine"),
                f("temperature", "℃", DataKind.INT16_S, 0.1, "temperature"));
        add(88, "3压力1温度", 1, 4, KIND_PRESSURE, "WMETER",
                f("pressure1", "MPa", DataKind.INT16_S, 0.001, "pressure"),
                f("pressure2", "MPa", DataKind.INT16_S, 0.001, "pressure"),
                f("pressure3", "MPa", DataKind.INT16_S, 0.001, "pressure"),
                f("temperature", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("flow", "m³/h", DataKind.INT16_S, 0.01, "flow"));
        add(89, "流量定位", 1, 8, KIND_FLOW, "WMETER",
                f("instantFlow", "m³/h", DataKind.FLOAT32, 1, "flow"),
                f("instantVelocity", "m/s", DataKind.FLOAT32, 1, "velocity"),
                f("positiveTotal", "m³", DataKind.INT32_S, 1, "flow"),
                f("netTotal", "m³", DataKind.INT32_S, 1, "flow"),
                f("dailyTotal", "m³", DataKind.INT32_S, 0.1, "flow"),
                f("longitude", "°", DataKind.FLOAT64, 1, "longitude"),
                f("latitude", "°", DataKind.FLOAT64, 1, "latitude"));
        add(90, "液位双路报警器", 2, 8, KIND_LEVEL, "LLOWT",
                f("level1", "m", DataKind.INT16_S, 0.01, "liquid-level"),
                f("level2", "m", DataKind.INT16_S, 0.01, "liquid-level"));
        add(91, "八通道温度", 1, 4, KIND_TEMP, "WMETER",
                f("temperature1", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("temperature2", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("temperature3", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("temperature4", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("temperature5", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("temperature6", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("temperature7", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("temperature8", "℃", DataKind.INT16_S, 0.1, "temperature"));
        add(92, "四通道温度", 1, 4, KIND_TEMP, "WMETER",
                f("temperature1", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("temperature2", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("temperature3", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("temperature4", "℃", DataKind.INT16_S, 0.1, "temperature"));
        add(93, "液位温湿度", 1, 4, KIND_LEVEL, "LLOWT",
                f("level", "m", DataKind.INT16_S, 0.01, "liquid-level"),
                f("humidity", "%RH", DataKind.INT16_S, 0.1, "humidity"),
                f("temperature", "℃", DataKind.INT16_S, 0.1, "temperature"));
        add(96, "空气质量CO/SO2", 1, 4, KIND_OTHER, "WMETER",
                f("temperature", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("humidity", "%RH", DataKind.INT16_S, 0.1, "humidity"),
                f("pm1", "ug/m³", DataKind.INT16_S, 1, "pm1"),
                f("pm25", "ug/m³", DataKind.INT16_S, 1, "pm25"),
                f("pm10", "ug/m³", DataKind.INT16_S, 1, "pm10"),
                f("co2", "ppm", DataKind.INT16_S, 1, "co2"),
                f("co", "ppm", DataKind.INT16_S, 1, "co"),
                f("so2", "ppm", DataKind.INT16_S, 0.01, "so2"));
        add(99, "可燃有毒气体", 1, 4, KIND_GAS, "WCGD",
                f("gasType", "", DataKind.INT16_S, 1, "gas-type"),
                f("sensorStatus", "", DataKind.INT16_S, 1, "switch"),
                f("concentration", "", DataKind.INT16_S, 1, "gas"));
        add(100, "开关模拟继电器采集", 1, 4, KIND_SWITCH, "WMETER",
                f("diBits", "", DataKind.INT16_U, 1, "switch"),
                f("current1", "mA", DataKind.INT16_U, 0.01, "current"),
                f("current2", "mA", DataKind.INT16_U, 0.01, "current"),
                f("current3", "mA", DataKind.INT16_U, 0.01, "current"),
                f("current4", "mA", DataKind.INT16_U, 0.01, "current"),
                f("current5", "mA", DataKind.INT16_U, 0.01, "current"),
                f("current6", "mA", DataKind.INT16_U, 0.01, "current"),
                f("current7", "mA", DataKind.INT16_U, 0.01, "current"),
                f("current8", "mA", DataKind.INT16_U, 0.01, "current"),
                f("doBits", "", DataKind.INT16_U, 1, "switch"));
        add(101, "控制器8DI4DO", 1, 4, KIND_SWITCH, "WMETER",
                f("channelCount", "", DataKind.INT16_S, 1, "switch"),
                f("diBits", "", DataKind.INT16_U, 1, "switch"),
                f("doBits", "", DataKind.INT16_U, 1, "switch"));
        add(102, "声光报警器", 1, 4, KIND_OTHER, "WMETER", f("alarmState", "", DataKind.INT16_S, 1, "switch"));
        add(105, "空气质量CO/NH3", 1, 4, KIND_OTHER, "WMETER",
                f("temperature", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("humidity", "%RH", DataKind.INT16_S, 0.1, "humidity"),
                f("pm1", "ug/m³", DataKind.INT16_S, 1, "pm1"),
                f("pm25", "ug/m³", DataKind.INT16_S, 1, "pm25"),
                f("pm10", "ug/m³", DataKind.INT16_S, 1, "pm10"),
                f("co2", "ppm", DataKind.INT16_S, 1, "co2"),
                f("co", "ppm", DataKind.INT16_S, 1, "co"),
                f("nh3", "ppm", DataKind.INT16_S, 0.1, "nh3"));
        add(106, "温度振动传感器", 1, 4, KIND_OTHER, "WMETER",
                f("temperature", "℃", DataKind.INT16_S, 0.1, "temperature"),
                f("velocityX", "mm/s", DataKind.INT16_S, 0.1, "vibration"),
                f("velocityY", "mm/s", DataKind.INT16_S, 0.1, "vibration"),
                f("velocityZ", "mm/s", DataKind.INT16_S, 0.1, "vibration"),
                f("accelX", "m/s²", DataKind.INT16_S, 0.1, "vibration"),
                f("accelY", "m/s²", DataKind.INT16_S, 0.1, "vibration"),
                f("accelZ", "m/s²", DataKind.INT16_S, 0.1, "vibration"),
                f("displaceX", "um", DataKind.INT16_S, 1, "displacement"),
                f("displaceY", "um", DataKind.INT16_S, 1, "displacement"),
                f("displaceZ", "um", DataKind.INT16_S, 1, "displacement"),
                f("vibrationAlarm", "", DataKind.INT16_S, 1, "switch"));
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
