package com.zhian.gateway.third.others.mk.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 铭控 V3.3 设备类型（表二）
 *
 * @author tongwenjin
 * @since 2026/9/3
 */
@Getter
@AllArgsConstructor
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

    private static final Map<Integer, MkV3Type> REGISTRY = new HashMap<>();

    static {
        add(0, "压力表kPa", 1, 2, 4, 0.1, "kPa", KIND_PRESSURE, "WMETER");
        add(1, "压力表MPa", 1, 2, 4, 0.001, "MPa", KIND_PRESSURE, "WMETER");
        add(2, "压力表BAR", 1, 2, 4, 0.01, "Bar", KIND_PRESSURE, "WMETER");
        add(3, "压力表PSI", 1, 2, 4, 0.1, "PSI", KIND_PRESSURE, "WMETER");
        add(4, "压力表Pa", 1, 2, 4, 1, "Pa", KIND_PRESSURE, "WMETER");
        add(5, "压力表mBAR", 1, 2, 4, 1, "mBAR", KIND_PRESSURE, "WMETER");
        add(6, "压力表Kgf/cm2", 1, 2, 4, 0.01, "Kgf/cm2", KIND_PRESSURE, "WMETER");
        add(7, "压力表mmHg", 1, 2, 4, 0.01, "mmHg", KIND_PRESSURE, "WMETER");
        add(8, "液位表m", 1, 2, 4, 0.01, "m", KIND_LEVEL, "LLOWT");
        add(9, "液位表cm", 1, 2, 4, 0.1, "cm", KIND_LEVEL, "LLOWT");
        add(10, "液位表mm", 1, 2, 4, 1, "mm", KIND_LEVEL, "LLOWT");
        add(11, "温度表℃", 1, 2, 4, 0.1, "℃", KIND_TEMP, "WMETER");
        add(12, "温度表℉", 1, 2, 4, 0.1, "℉", KIND_TEMP, "WMETER");
        add(13, "湿度表%RH", 1, 2, 4, 0.1, "%RH", KIND_HUMIDITY, "WMETER");
        add(14, "电流表", 1, 2, 4, 0.1, "A", KIND_OTHER, "WMETER");
        add(15, "电压表", 1, 2, 4, 0.1, "V", KIND_OTHER, "WMETER");
        add(16, "差压表", 2, 4, 8, 0.001, "MPa", KIND_PRESSURE, "WMETER");
        add(17, "温压表", 2, 4, 8, 0.001, "MPa", KIND_PRESSURE, "WMETER");
        add(18, "温湿度表", 2, 4, 8, 0.1, "%RH", KIND_HUMIDITY, "WMETER");
        add(19, "振动", 1, 2, 4, 0.1, "mm/s", KIND_OTHER, "WMETER");
        add(20, "水浸", 1, 2, 4, 1, "", KIND_WATER, "WWIS");
        add(21, "差压表进出压", 2, 4, 8, 1, "kPa", KIND_PRESSURE, "WMETER");
        add(22, "压力表10Pa", 1, 2, 4, 10, "Pa", KIND_PRESSURE, "WMETER");
        add(23, "倾角", 1, 2, 4, 1, "°", KIND_OTHER, "WMETER");
        add(24, "消火栓", 2, 4, 8, 0.001, "MPa", KIND_HYDRANT, "OFHAG");
        add(25, "双温度表", 2, 4, 8, 0.1, "℃", KIND_TEMP, "WMETER");
        add(26, "力KN", 1, 2, 4, 1, "KN", KIND_OTHER, "WMETER");
        add(27, "流量计", 1, 16, 8, 1, "m³/h", KIND_FLOW, "WMETER");
        add(28, "16通道开关量", 1, 2, 4, 1, "", KIND_SWITCH, "WMETER");
        add(29, "液位超声波探头", 1, 6, 4, 0.01, "m", KIND_LEVEL, "LLOWT");
        add(30, "超声波液位表", 1, 2, 4, 0.01, "m", KIND_LEVEL, "LLOWT");
        add(31, "可燃气体", 1, 2, 4, 0.1, "%LEL", KIND_GAS, "WCGD");
        add(32, "定位坐标", 1, 16, 16, 1, "°", KIND_OTHER, "WMETER");
        add(33, "液位压力表", 2, 4, 8, 0.01, "m", KIND_LEVEL, "LLOWT");
        add(34, "消火栓压力", 1, 2, 4, 0.001, "MPa", KIND_HYDRANT, "OFHAG");
        add(35, "差压表kPa", 1, 2, 4, 0.1, "kPa", KIND_PRESSURE, "WMETER");
        add(36, "消防栓闷盖", 2, 4, 8, 1, "", KIND_HYDRANT, "OFHAG");
        add(37, "井盖状态仪", 1, 2, 4, 1, "", KIND_WELL, "OFHMC");
        add(38, "液位温度表", 2, 4, 8, 0.01, "m", KIND_LEVEL, "LLOWT");
        add(39, "温度异动", 2, 4, 8, 0.1, "℃", KIND_TEMP, "WMETER");
        add(40, "井盖液位", 1, 2, 4, 0.01, "m", KIND_WELL, "OFHMC");
        add(41, "噪声", 1, 2, 4, 1, "dB", KIND_OTHER, "WMETER");
        add(42, "雷达测距仪", 1, 2, 4, 0.01, "m", KIND_LEVEL, "LLOWT");
        add(43, "32通道开关量", 2, 4, 8, 1, "", KIND_SWITCH, "WMETER");
        add(44, "风速仪", 1, 6, 4, 1, "Pa", KIND_OTHER, "WMETER");
        add(46, "磁致伸缩液位计", 1, 2, 4, 0.001, "mm", KIND_LEVEL, "LLOWT");
        add(47, "PH检测仪", 1, 2, 4, 0.01, "PH", KIND_OTHER, "WMETER");
        add(48, "电导率检测仪", 1, 2, 4, 0.1, "uS/cm", KIND_OTHER, "WMETER");
        add(49, "液位倾角", 2, 4, 8, 0.01, "m", KIND_LEVEL, "LLOWT");
        add(50, "4通道压力温度", 1, 10, 4, 0.001, "MPa", KIND_PRESSURE, "WMETER");
        add(51, "ORP检测仪", 1, 2, 4, 1, "mV", KIND_OTHER, "WMETER");
        add(52, "力KN-32位", 1, 4, 8, 1, "KN", KIND_OTHER, "WMETER");
        add(53, "流量计m3", 1, 4, 8, 0.01, "m³", KIND_FLOW, "WMETER");
        add(54, "氨氮检测仪", 1, 6, 4, 0.1, "mg/L", KIND_OTHER, "WMETER");
        add(55, "COD检测仪", 1, 6, 4, 0.1, "mg/L", KIND_OTHER, "WMETER");
        add(56, "溶解氧检测仪", 1, 6, 4, 0.01, "mg/L", KIND_OTHER, "WMETER");
        add(57, "浊度检测仪", 1, 2, 4, 0.1, "NTU", KIND_OTHER, "WMETER");
        add(58, "消火栓流量", 1, 8, 4, 0.001, "MPa", KIND_HYDRANT, "OFHAG");
        add(59, "门磁开关", 1, 2, 4, 1, "", KIND_SWITCH, "WMETER");
        add(60, "电表", 1, 20, 4, 0.01, "kWh", KIND_OTHER, "WMETER");
        add(61, "压力0.01MPa", 1, 2, 4, 0.01, "MPa", KIND_PRESSURE, "WMETER");
        add(62, "振弦温度", 2, 8, 16, 0.01, "", KIND_OTHER, "WMETER");
        add(63, "液位0.001m", 2, 2, 8, 0.001, "m", KIND_LEVEL, "LLOWT");
        add(64, "蓝绿藻", 2, 8, 16, 1, "cell/mL", KIND_OTHER, "WMETER");
        add(65, "三通道温度", 1, 6, 4, 0.1, "℃", KIND_TEMP, "WMETER");
        add(66, "双通道压力", 2, 4, 8, 0.001, "MPa", KIND_PRESSURE, "WMETER");
        add(67, "智能井盖监测终端", 2, 4, 8, 1, "°", KIND_WELL, "OFHMC");
        add(68, "PH监测终端", 2, 4, 8, 0.01, "pH", KIND_OTHER, "WMETER");
        add(69, "电导率监测终端", 2, 8, 16, 0.1, "uS/cm", KIND_OTHER, "WMETER");
        add(70, "拉线位移", 1, 4, 8, 0.01, "mm", KIND_OTHER, "WMETER");
        add(71, "水质分析仪", 1, 10, 4, 0.1, "mg/L", KIND_OTHER, "WMETER");
        add(72, "智能空气质量", 1, 16, 4, 0.1, "℃", KIND_OTHER, "WMETER");
        add(73, "燃气流量表", 1, 34, 8, 1, "m³/H", KIND_FLOW, "WMETER");
        add(74, "多普勒流速仪", 1, 6, 4, 0.001, "m/s", KIND_FLOW, "WMETER");
        add(75, "光照度", 1, 2, 4, 1, "lux", KIND_OTHER, "WMETER");
        add(76, "光照度无符号", 1, 2, 4, 1, "lux", KIND_OTHER, "WMETER");
        add(77, "乙醇", 1, 2, 4, 1, "ppm", KIND_GAS, "WCGD");
        add(78, "空气质量O2", 1, 16, 4, 0.1, "℃", KIND_OTHER, "WMETER");
        add(79, "空气质量S", 1, 16, 4, 0.1, "℃", KIND_OTHER, "WMETER");
        add(80, "温度0.01℃", 1, 2, 4, 0.01, "℃", KIND_TEMP, "WMETER");
        add(81, "气体探测器", 1, 2, 4, 1, "umol/mol", KIND_GAS, "WCGD");
        add(83, "土壤温湿度", 1, 6, 4, 0.1, "%RH", KIND_OTHER, "WMETER");
        add(84, "雷达液位计", 1, 4, 8, 0.001, "m", KIND_LEVEL, "LLOWT");
        add(86, "振弦温度Hz", 2, 8, 16, 0.1, "Hz", KIND_OTHER, "WMETER");
        add(87, "余氯检测仪", 2, 4, 8, 0.01, "mg/L", KIND_OTHER, "WMETER");
        add(88, "3压力1温度", 1, 10, 4, 0.001, "MPa", KIND_PRESSURE, "WMETER");
        add(89, "流量定位", 1, 36, 8, 1, "m³/h", KIND_FLOW, "WMETER");
        add(90, "液位双路报警器", 2, 4, 8, 0.01, "m", KIND_LEVEL, "LLOWT");
        add(91, "八通道温度", 1, 16, 4, 0.1, "℃", KIND_TEMP, "WMETER");
        add(92, "四通道温度", 1, 8, 4, 0.1, "℃", KIND_TEMP, "WMETER");
        add(93, "液位温湿度", 1, 6, 4, 0.01, "m", KIND_LEVEL, "LLOWT");
        add(96, "空气质量CO/SO2", 1, 16, 4, 0.1, "℃", KIND_OTHER, "WMETER");
        add(99, "可燃有毒气体", 1, 6, 4, 1, "ppm", KIND_GAS, "WCGD");
        add(100, "开关模拟继电器采集", 1, 20, 4, 0.01, "mA", KIND_SWITCH, "WMETER");
        add(101, "控制器8DI4DO", 1, 6, 4, 1, "", KIND_SWITCH, "WMETER");
        add(102, "声光报警器", 1, 2, 4, 1, "", KIND_OTHER, "WMETER");
        add(105, "空气质量CO/NH3", 1, 16, 4, 0.1, "℃", KIND_OTHER, "WMETER");
        add(106, "温度振动传感器", 1, 22, 4, 0.1, "℃", KIND_OTHER, "WMETER");
    }

    private final int code;
    private final String name;
    private final int alarmGroups;
    // 实时数据字节
    private final int dataBytes;
    // 报警参数字节
    private final int alarmBytes;
    private final double lsb;
    private final String unit;
    private final String kind;
    private final String typeCode;

    public static MkV3Type of(int code) {
        return REGISTRY.get(code);
    }

    private static void add(int code, String name, int alarmGroups, int dataBytes, int alarmBytes,
                            double lsb, String unit, String kind, String typeCode) {
        REGISTRY.put(code, new MkV3Type(code, name, alarmGroups, dataBytes, alarmBytes, lsb, unit, kind, typeCode));
    }
}
