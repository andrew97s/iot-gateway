package com.zhian.gateway.third.others.mklora.protocol;


import com.zhian.gateway.third.others.mklora.vo.MkLoraSensorValue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * 表二设备类型与实时数据区解析。
 *
 * <p>仅覆盖主动上报数据区换算；复杂/专有结构在无法完整建模时保留 raw hex。</p>
 */
public final class MkLoraSensorTypes {

    public enum DataKind {
        INT16_S,
        INT16_U,
        INT32_S,
        INT32_U,
        FLOAT32,
        FLOAT64,
        RAW
    }

    public static final class FieldDef {
        private final String name;
        private final String unit;
        private final DataKind kind;
        /** 工程值 = raw * scale；浮点类型忽略 */
        private final BigDecimal scale;
        private final int scaleDigits;

        public FieldDef(String name, String unit, DataKind kind, String scale) {
            this.name = name;
            this.unit = unit;
            this.kind = kind;
            this.scale = scale == null ? BigDecimal.ONE : new BigDecimal(scale);
            this.scaleDigits = this.scale.stripTrailingZeros().scale() < 0
                    ? 0
                    : Math.max(0, this.scale.stripTrailingZeros().scale());
        }

        public String getName() {
            return name;
        }

        public String getUnit() {
            return unit;
        }

        public DataKind getKind() {
            return kind;
        }

        public BigDecimal getScale() {
            return scale;
        }

        public int getScaleDigits() {
            return scaleDigits;
        }

        public int byteLength() {
            switch (kind) {
                case INT16_S:
                case INT16_U:
                    return 2;
                case INT32_S:
                case INT32_U:
                case FLOAT32:
                    return 4;
                case FLOAT64:
                    return 8;
                case RAW:
                default:
                    return -1;
            }
        }
    }

    public static final class TypeDef {
        private final int type;
        private final String name;
        private final int dataLength;
        private final List<FieldDef> fields;

        public TypeDef(int type, String name, int dataLength, FieldDef... fields) {
            this.type = type;
            this.name = name;
            this.dataLength = dataLength;
            List<FieldDef> list = new ArrayList<>();
            if (fields != null) {
                Collections.addAll(list, fields);
            }
            this.fields = Collections.unmodifiableList(list);
        }

        public int getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public int getDataLength() {
            return dataLength;
        }

        public List<FieldDef> getFields() {
            return fields;
        }
    }

    private static final Map<Integer, TypeDef> TYPES = new HashMap<>();

    static {
        // 压力
        reg(0, "压力表", 2, f("pressure", "kPa", DataKind.INT16_S, "0.1"));
        reg(1, "压力表", 2, f("pressure", "MPa", DataKind.INT16_S, "0.001"));
        reg(2, "压力表", 2, f("pressure", "BAR", DataKind.INT16_S, "0.01"));
        reg(3, "压力表", 2, f("pressure", "PSI", DataKind.INT16_S, "0.1"));
        reg(4, "压力表", 2, f("pressure", "Pa", DataKind.INT16_S, "1"));
        reg(5, "压力表", 2, f("pressure", "mBAR", DataKind.INT16_S, "1"));
        reg(6, "压力表", 2, f("pressure", "Kgf/cm2", DataKind.INT16_S, "0.01"));
        reg(7, "压力表", 2, f("pressure", "mmHg", DataKind.INT16_S, "0.01"));
        // 液位
        reg(8, "液位表", 2, f("level", "m", DataKind.INT16_S, "0.01"));
        reg(9, "液位表", 2, f("level", "cm", DataKind.INT16_S, "0.1"));
        reg(10, "液位表", 2, f("level", "mm", DataKind.INT16_S, "1"));
        // 温湿度等
        reg(11, "温度表", 2, f("temperature", "℃", DataKind.INT16_S, "0.1"));
        reg(12, "温度表", 2, f("temperature", "℉", DataKind.INT16_S, "0.1"));
        reg(13, "湿度表", 2, f("humidity", "%RH", DataKind.INT16_S, "0.1"));
        reg(14, "电流表", 2, f("current", "A", DataKind.INT16_S, "0.1"));
        reg(15, "电压表", 2, f("voltage", "V", DataKind.INT16_S, "0.1"));
        reg(16, "差压表", 4,
                f("staticPressure", "MPa", DataKind.INT16_S, "0.001"),
                f("diffPressure", "kPa", DataKind.INT16_S, "0.1"));
        reg(17, "温压表", 4,
                f("pressure", "MPa", DataKind.INT16_S, "0.001"),
                f("temperature", "℃", DataKind.INT16_S, "0.1"));
        reg(18, "温湿度表", 4,
                f("humidity", "%RH", DataKind.INT16_S, "0.1"),
                f("temperature", "℃", DataKind.INT16_S, "0.1"));
        reg(19, "振动", 2, f("vibration", "mm/s", DataKind.INT16_S, "0.1"));
        reg(20, "水浸", 2, f("waterImmersion", "", DataKind.INT16_S, "1"));
        reg(21, "差压表", 4,
                f("inPressure", "kPa", DataKind.INT16_S, "1"),
                f("returnPressure", "kPa", DataKind.INT16_S, "1"));
        reg(22, "压力表", 2, f("pressure", "Pa", DataKind.INT16_S, "10"));
        reg(23, "倾角", 2, f("tilt", "°", DataKind.INT16_S, "1"));
        reg(24, "消火栓", 4,
                f("pressure", "MPa", DataKind.INT16_S, "0.001"),
                f("tilt", "°", DataKind.INT16_S, "1"));
        reg(25, "双温度表", 4,
                f("temperature1", "℃", DataKind.INT16_S, "0.1"),
                f("temperature2", "℃", DataKind.INT16_S, "0.1"));
        reg(26, "力", 2, f("force", "KN", DataKind.INT16_S, "1"));
        reg(27, "流量计", 16,
                f("instantFlow", "m³/h", DataKind.FLOAT32, null),
                f("instantVelocity", "m/s", DataKind.FLOAT32, null),
                f("positiveTotal", "m³", DataKind.INT32_U, "1"),
                f("netTotal", "m³", DataKind.INT32_U, "1"));
        reg(28, "16通道开关量", 2, f("diBits", "", DataKind.INT16_U, "1"));
        reg(29, "液位表(超声&探头)", 6,
                f("probeLevel", "m", DataKind.INT16_S, "0.01"),
                f("ultrasonicLevel", "m", DataKind.INT16_S, "0.01"),
                f("convertedLevel", "m", DataKind.INT16_S, "0.01"));
        reg(30, "超声波液位表", 2, f("level", "m", DataKind.INT16_S, "0.01"));
        reg(31, "可燃气体", 2, f("gas", "%LEL", DataKind.INT16_U, "0.1"));
        reg(32, "定位坐标", 16,
                f("longitude", "°", DataKind.FLOAT64, null),
                f("latitude", "°", DataKind.FLOAT64, null));
        reg(33, "液位&压力表", 4,
                f("level", "m", DataKind.INT16_S, "0.01"),
                f("pressure", "MPa", DataKind.INT16_S, "0.001"));
        reg(34, "消火栓压力", 2, f("pressure", "MPa", DataKind.INT16_S, "0.001"));
        reg(35, "差压表", 2, f("diffPressure", "kPa", DataKind.INT16_S, "0.1"));
        reg(36, "消防栓闷盖", 4,
                f("waterAd", "", DataKind.INT16_S, "1"),
                f("coverAd", "", DataKind.INT16_S, "1"));
        reg(37, "井盖状态仪", 2, f("switchAd", "", DataKind.INT16_S, "1"));
        reg(38, "液位&温度表", 4,
                f("level", "m", DataKind.INT16_S, "0.01"),
                f("temperature", "℃", DataKind.INT16_S, "0.1"));
        reg(39, "温度&异动", 4,
                f("temperature", "℃", DataKind.INT16_S, "0.1"),
                f("switchAd", "", DataKind.INT16_S, "1"));
        reg(40, "井盖液位", 2, f("level", "m", DataKind.INT16_S, "0.01"));
        reg(41, "噪声", 2, f("noise", "dB", DataKind.INT16_S, "1"));
        reg(42, "雷达测距仪", 2, f("distance", "m", DataKind.INT16_S, "0.01"));
        reg(43, "32通道开关量", 4, f("diBits", "", DataKind.INT32_U, "1"));
        reg(44, "风速仪", 6,
                f("diffPressure", "Pa", DataKind.INT16_S, "1"),
                f("windSpeed", "m/s", DataKind.INT16_S, "0.01"),
                f("airVolume", "m³/m", DataKind.INT16_S, "1"));
        // 45 MODBUS 变长，运行时按实际长度 RAW
        reg(45, "MODBUS采集终端", -1, f("modbusPayload", "", DataKind.RAW, null));
        reg(46, "磁致伸缩液位计", 2, f("level", "mm", DataKind.INT16_S, "0.001"));
        reg(47, "PH检测仪", 2, f("ph", "PH", DataKind.INT16_S, "0.01"));
        reg(48, "电导率检测仪", 2, f("conductivity", "uS/cm", DataKind.INT16_S, "0.1"));
        reg(49, "液位&倾角", 4,
                f("level", "m", DataKind.INT16_S, "0.01"),
                f("tilt", "°", DataKind.INT16_S, "1"));
        reg(50, "4通道压力温度检测仪", 10,
                f("pressure1", "MPa", DataKind.INT16_S, "0.001"),
                f("pressure2", "MPa", DataKind.INT16_S, "0.001"),
                f("temperature1", "℃", DataKind.INT16_S, "0.1"),
                f("temperature2", "℃", DataKind.INT16_S, "0.1"),
                f("flow", "m³/h", DataKind.INT16_S, "0.01"));
        reg(51, "ORP检测仪", 2, f("orp", "mVpH", DataKind.INT16_S, "1"));
        reg(52, "力", 4, f("force", "KN", DataKind.INT32_S, "1"));
        reg(53, "流量计", 4, f("totalFlow", "m3", DataKind.INT32_U, "0.01"));
        reg(54, "氨氮检测仪", 6,
                f("ammonia", "mg/L", DataKind.INT16_S, "0.1"),
                f("ph", "pH", DataKind.INT16_S, "0.1"),
                f("temperature", "℃", DataKind.INT16_S, "0.1"));
        reg(55, "COD检测仪", 6,
                f("cod", "mg/L", DataKind.INT16_S, "0.1"),
                f("turbidity", "NTU", DataKind.INT16_S, "0.1"),
                f("temperature", "℃", DataKind.INT16_S, "0.1"));
        reg(56, "溶解氧检测仪", 6,
                f("dissolvedOxygen", "mg/L", DataKind.INT16_S, "0.01"),
                f("temperature", "℃", DataKind.INT16_S, "0.1"),
                f("doUnit", "", DataKind.INT16_S, "1"));
        reg(57, "浊度检测仪", 2, f("turbidity", "NTU", DataKind.INT16_S, "0.1"));
        reg(58, "消火栓流量", 8,
                f("pressure", "MPa", DataKind.INT16_S, "0.001"),
                f("tilt", "°", DataKind.INT16_S, "1"),
                f("flow", "L/s", DataKind.INT16_S, "0.01"),
                f("temperature", "℃", DataKind.INT16_S, "0.1"));
        reg(59, "门磁开关", 2, f("infraredCount", "", DataKind.INT16_U, "1"));
        reg(60, "电表", 20,
                f("activeEnergy", "kWh", DataKind.INT32_U, "0.01"),
                f("voltageA", "V", DataKind.INT16_U, "0.1"),
                f("voltageB", "V", DataKind.INT16_U, "0.1"),
                f("voltageC", "V", DataKind.INT16_U, "0.1"),
                f("currentA", "A", DataKind.INT16_U, "0.01"),
                f("currentB", "A", DataKind.INT16_U, "0.01"),
                f("currentC", "A", DataKind.INT16_U, "0.01"),
                f("activePower", "kW", DataKind.INT16_U, "0.001"),
                f("powerFactor", "", DataKind.INT16_U, "0.001"));
        reg(61, "压力", 2, f("pressure", "MPa", DataKind.INT16_U, "0.01"));
        reg(62, "振弦温度", 8,
                f("vibratingWire", "", DataKind.INT32_S, "0.01"),
                f("temperature", "℃", DataKind.INT32_S, "0.1"));
        reg(63, "液位", 2, f("level", "m", DataKind.INT16_U, "0.001"));
        reg(64, "蓝绿藻", 8,
                f("algae", "cell/mL", DataKind.INT32_S, "1"),
                f("temperature", "℃", DataKind.INT32_S, "0.1"));
        reg(65, "三通道温度", 6,
                f("temperature1", "℃", DataKind.INT16_S, "0.1"),
                f("temperature2", "℃", DataKind.INT16_S, "0.1"),
                f("temperature3", "℃", DataKind.INT16_S, "0.1"));
        reg(66, "双通道压力", 4,
                f("pressure1", "MPa", DataKind.INT16_S, "0.001"),
                f("pressure2", "kPa", DataKind.INT16_S, "0.01"));
        reg(67, "智能井盖监测终端", 4,
                f("waterImmersion", "", DataKind.INT16_S, "1"),
                f("tilt", "°", DataKind.INT16_S, "1"));
        reg(68, "PH监测终端", 4,
                f("ph", "pH", DataKind.INT16_S, "0.01"),
                f("temperature", "℃", DataKind.INT16_S, "0.1"));
        reg(69, "电导率监测终端", 8,
                f("conductivity", "uS/cm", DataKind.INT32_S, "0.1"),
                f("temperature", "℃", DataKind.INT32_S, "0.1"));
        reg(70, "拉线位移", 4, f("displacement", "mm", DataKind.INT32_S, "0.01"));
        reg(71, "水质分析仪", 10,
                f("ammonia", "mg/L", DataKind.INT16_S, "0.1"),
                f("nitrate", "mg/L", DataKind.INT16_S, "0.1"),
                f("chloride", "mg/L", DataKind.INT16_S, "0.1"),
                f("ph", "pH", DataKind.INT16_S, "0.01"),
                f("temperature", "℃", DataKind.INT16_S, "0.1"));
        reg(72, "智能空气质量监测终端", 16,
                f("temperature", "℃", DataKind.INT16_S, "0.1"),
                f("humidity", "%RH", DataKind.INT16_S, "0.1"),
                f("pm1", "ug/m³", DataKind.INT16_S, "1"),
                f("pm25", "ug/m³", DataKind.INT16_S, "1"),
                f("pm10", "ug/m³", DataKind.INT16_S, "1"),
                f("co2", "ppm", DataKind.INT16_S, "1"),
                f("ch2o", "mg/m³", DataKind.INT16_S, "0.001"),
                f("tvoc", "mg/m³", DataKind.INT16_S, "0.01"));
        reg(73, "燃气流量表", 34,
                f("standardFlow", "m³/H", DataKind.FLOAT32, null),
                f("workingFlow", "m³/H", DataKind.FLOAT32, null),
                f("standardTotal", "m³", DataKind.FLOAT64, null),
                f("workingTotal", "m³", DataKind.FLOAT64, null),
                f("temperature", "℃", DataKind.FLOAT32, null),
                f("pressure", "kPa", DataKind.FLOAT32, null),
                f("status", "", DataKind.INT16_S, "1"));
        reg(74, "多普勒流速仪", 6,
                f("velocity", "m/s", DataKind.INT16_S, "0.001"),
                f("level", "m", DataKind.INT16_S, "0.001"),
                f("temperature", "℃", DataKind.INT16_S, "0.1"));
        reg(75, "光照度", 2, f("illuminance", "lux", DataKind.INT16_S, "1"));
        reg(76, "光照度", 2, f("illuminance", "lux", DataKind.INT16_U, "1"));
        reg(77, "乙醇", 2, f("ethanol", "ppm", DataKind.INT16_S, "1"));
        reg(78, "智能空气质量监测终端(O2)", 16,
                f("temperature", "℃", DataKind.INT16_S, "0.1"),
                f("humidity", "%RH", DataKind.INT16_S, "0.1"),
                f("pm1", "ug/m³", DataKind.INT16_S, "1"),
                f("pm25", "ug/m³", DataKind.INT16_S, "1"),
                f("pm10", "ug/m³", DataKind.INT16_S, "1"),
                f("co2", "ppm", DataKind.INT16_S, "1"),
                f("o2", "%", DataKind.INT16_S, "0.1"),
                f("tvoc", "mg/m³", DataKind.INT16_S, "0.01"));
        reg(79, "智能空气质量监测终端(S)", 16,
                f("temperature", "℃", DataKind.INT16_S, "0.1"),
                f("humidity", "%RH", DataKind.INT16_S, "0.1"),
                f("pm1", "ug/m³", DataKind.INT16_S, "1"),
                f("pm25", "ug/m³", DataKind.INT16_S, "1"),
                f("pm10", "ug/m³", DataKind.INT16_S, "1"),
                f("h2s", "ppm", DataKind.INT16_S, "0.01"),
                f("nh3", "ppm", DataKind.INT16_S, "0.1"),
                f("so2", "ppm", DataKind.INT16_S, "0.01"));
        reg(80, "温度表", 2, f("temperature", "℃", DataKind.INT16_S, "0.01"));
        reg(81, "气体探测器", 2, f("gas", "umol/mol", DataKind.INT16_S, "1"));
        reg(83, "土壤温湿度", 6,
                f("humidity", "%RH", DataKind.INT16_S, "0.1"),
                f("temperature", "℃", DataKind.INT16_S, "0.1"),
                f("conductivity", "uS/cm", DataKind.INT16_S, "1"));
        reg(84, "雷达液位计", 4, f("level", "m", DataKind.INT32_S, "0.001"));
        reg(86, "振弦温度", 8,
                f("frequency", "Hz", DataKind.INT32_S, "0.1"),
                f("temperature", "℃", DataKind.INT32_S, "0.1"));
        reg(87, "余氯检测仪", 4,
                f("residualChlorine", "mg/L", DataKind.INT16_S, "0.01"),
                f("temperature", "℃", DataKind.INT16_S, "0.1"));
        reg(88, "3压力1温度检测仪", 10,
                f("pressure1", "MPa", DataKind.INT16_S, "0.001"),
                f("pressure2", "MPa", DataKind.INT16_S, "0.001"),
                f("pressure3", "MPa", DataKind.INT16_S, "0.001"),
                f("temperature", "℃", DataKind.INT16_S, "0.1"),
                f("flow", "m³/h", DataKind.INT16_S, "0.01"));
        reg(89, "流量定位", 36,
                f("instantFlow", "m³/h", DataKind.FLOAT32, null),
                f("instantVelocity", "m/s", DataKind.FLOAT32, null),
                f("positiveTotal", "m³", DataKind.INT32_S, "1"),
                f("netTotal", "m³", DataKind.INT32_S, "1"),
                f("dailyTotal", "m³", DataKind.INT32_S, "0.1"),
                f("longitude", "°", DataKind.FLOAT64, null),
                f("latitude", "°", DataKind.FLOAT64, null));
        reg(90, "液位双路报警器", 4,
                f("level1", "m", DataKind.INT16_S, "0.01"),
                f("level2", "m", DataKind.INT16_S, "0.01"));
        reg(91, "八通道温度", 16,
                f("temperature1", "℃", DataKind.INT16_S, "0.1"),
                f("temperature2", "℃", DataKind.INT16_S, "0.1"),
                f("temperature3", "℃", DataKind.INT16_S, "0.1"),
                f("temperature4", "℃", DataKind.INT16_S, "0.1"),
                f("temperature5", "℃", DataKind.INT16_S, "0.1"),
                f("temperature6", "℃", DataKind.INT16_S, "0.1"),
                f("temperature7", "℃", DataKind.INT16_S, "0.1"),
                f("temperature8", "℃", DataKind.INT16_S, "0.1"));
        reg(92, "四通道温度", 8,
                f("temperature1", "℃", DataKind.INT16_S, "0.1"),
                f("temperature2", "℃", DataKind.INT16_S, "0.1"),
                f("temperature3", "℃", DataKind.INT16_S, "0.1"),
                f("temperature4", "℃", DataKind.INT16_S, "0.1"));
        reg(93, "液位温湿度", 6,
                f("level", "m", DataKind.INT16_S, "0.01"),
                f("humidity", "%RH", DataKind.INT16_S, "0.1"),
                f("temperature", "℃", DataKind.INT16_S, "0.1"));
        reg(96, "智能空气质量监测终端(CO/SO2)", 16,
                f("temperature", "℃", DataKind.INT16_S, "0.1"),
                f("humidity", "%RH", DataKind.INT16_S, "0.1"),
                f("pm1", "ug/m³", DataKind.INT16_S, "1"),
                f("pm25", "ug/m³", DataKind.INT16_S, "1"),
                f("pm10", "ug/m³", DataKind.INT16_S, "1"),
                f("co2", "ppm", DataKind.INT16_S, "1"),
                f("co", "ppm", DataKind.INT16_S, "1"),
                f("so2", "ppm", DataKind.INT16_S, "0.01"));
        reg(99, "可燃(有毒)气体", 6,
                f("gasType", "", DataKind.INT16_S, "1"),
                f("sensorStatus", "", DataKind.INT16_S, "1"),
                f("concentration", "", DataKind.INT16_S, "1"));
        reg(100, "16DI+8AI+4DO采集终端", 20,
                f("diBits", "", DataKind.INT16_U, "1"),
                f("current1", "mA", DataKind.INT16_U, "0.01"),
                f("current2", "mA", DataKind.INT16_U, "0.01"),
                f("current3", "mA", DataKind.INT16_U, "0.01"),
                f("current4", "mA", DataKind.INT16_U, "0.01"),
                f("current5", "mA", DataKind.INT16_U, "0.01"),
                f("current6", "mA", DataKind.INT16_U, "0.01"),
                f("current7", "mA", DataKind.INT16_U, "0.01"),
                f("current8", "mA", DataKind.INT16_U, "0.01"),
                f("doBits", "", DataKind.INT16_U, "1"));
        reg(101, "控制器8DI_4DO", 6,
                f("channelCount", "", DataKind.INT16_S, "1"),
                f("diBits", "", DataKind.INT16_U, "1"),
                f("doBits", "", DataKind.INT16_U, "1"));
        reg(102, "声光报警器", 2, f("alarmState", "", DataKind.INT16_S, "1"));
        reg(105, "智能空气质量监测终端(CO/NH3)", 16,
                f("temperature", "℃", DataKind.INT16_S, "0.1"),
                f("humidity", "%RH", DataKind.INT16_S, "0.1"),
                f("pm1", "ug/m³", DataKind.INT16_S, "1"),
                f("pm25", "ug/m³", DataKind.INT16_S, "1"),
                f("pm10", "ug/m³", DataKind.INT16_S, "1"),
                f("co2", "ppm", DataKind.INT16_S, "1"),
                f("co", "ppm", DataKind.INT16_S, "1"),
                f("nh3", "ppm", DataKind.INT16_S, "0.1"));
        reg(106, "温度振动传感器", 22,
                f("temperature", "℃", DataKind.INT16_S, "0.1"),
                f("velocityX", "mm/s", DataKind.INT16_S, "0.1"),
                f("velocityY", "mm/s", DataKind.INT16_S, "0.1"),
                f("velocityZ", "mm/s", DataKind.INT16_S, "0.1"),
                f("accelX", "m/s²", DataKind.INT16_S, "0.1"),
                f("accelY", "m/s²", DataKind.INT16_S, "0.1"),
                f("accelZ", "m/s²", DataKind.INT16_S, "0.1"),
                f("displaceX", "um", DataKind.INT16_S, "1"),
                f("displaceY", "um", DataKind.INT16_S, "1"),
                f("displaceZ", "um", DataKind.INT16_S, "1"),
                f("vibrationAlarm", "", DataKind.INT16_S, "1"));
    }

    private MkLoraSensorTypes() {
    }

    public static TypeDef get(int type) {
        return TYPES.get(type);
    }

    public static String typeName(int type) {
        TypeDef def = TYPES.get(type);
        return def == null ? "未知类型(" + type + ")" : def.getName();
    }

    public static List<MkLoraSensorValue> parseValues(int sensorType, byte[] data) {
        List<MkLoraSensorValue> values = new ArrayList<>();
        if (data == null || data.length == 0) {
            return values;
        }
        TypeDef typeDef = TYPES.get(sensorType);
        if (typeDef == null || typeDef.getFields().isEmpty()
                || typeDef.getFields().get(0).getKind() == DataKind.RAW
                || (typeDef.getDataLength() >= 0 && data.length < expectedMin(typeDef))) {
            values.add(MkLoraSensorValue.builder()
                    .name("raw")
                    .unit("")
                    .hex(toHex(data))
                    .build());
            return values;
        }

        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
        try {
            for (FieldDef field : typeDef.getFields()) {
                values.add(readField(buf, field));
            }
            if (buf.hasRemaining()) {
                byte[] remain = new byte[buf.remaining()];
                buf.get(remain);
                values.add(MkLoraSensorValue.builder()
                        .name("extra")
                        .hex(toHex(remain))
                        .build());
            }
        } catch (Exception e) {
            values.clear();
            values.add(MkLoraSensorValue.builder()
                    .name("raw")
                    .hex(toHex(data))
                    .build());
        }
        return values;
    }

    private static int expectedMin(TypeDef typeDef) {
        int sum = 0;
        for (FieldDef field : typeDef.getFields()) {
            int len = field.byteLength();
            if (len < 0) {
                return 0;
            }
            sum += len;
        }
        return sum;
    }

    private static MkLoraSensorValue readField(ByteBuffer buf, FieldDef field) {
        switch (field.getKind()) {
            case INT16_S: {
                short raw = buf.getShort();
                return scaled(field, raw);
            }
            case INT16_U: {
                int raw = buf.getShort() & 0xFFFF;
                return scaled(field, raw);
            }
            case INT32_S: {
                int raw = buf.getInt();
                return scaled(field, raw);
            }
            case INT32_U: {
                long raw = buf.getInt() & 0xFFFFFFFFL;
                return scaled(field, raw);
            }
            case FLOAT32: {
                float raw = buf.getFloat();
                return MkLoraSensorValue.builder()
                        .name(field.getName())
                        .unit(field.getUnit())
                        .value(BigDecimal.valueOf(raw).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros())
                        .build();
            }
            case FLOAT64: {
                double raw = buf.getDouble();
                return MkLoraSensorValue.builder()
                        .name(field.getName())
                        .unit(field.getUnit())
                        .value(BigDecimal.valueOf(raw).setScale(8, RoundingMode.HALF_UP).stripTrailingZeros())
                        .build();
            }
            case RAW:
            default: {
                byte[] remain = new byte[buf.remaining()];
                buf.get(remain);
                return MkLoraSensorValue.builder()
                        .name(field.getName())
                        .unit(field.getUnit())
                        .hex(toHex(remain))
                        .build();
            }
        }
    }

    private static MkLoraSensorValue scaled(FieldDef field, long raw) {
        BigDecimal value = BigDecimal.valueOf(raw).multiply(field.getScale());
        int digits = Math.max(field.getScaleDigits(), 0);
        if (digits > 0) {
            value = value.setScale(digits, RoundingMode.HALF_UP);
        }
        return MkLoraSensorValue.builder()
                .name(field.getName())
                .unit(field.getUnit())
                .raw(raw)
                .value(value)
                .build();
    }

    private static FieldDef f(String name, String unit, DataKind kind, String scale) {
        return new FieldDef(name, unit, kind, scale);
    }

    private static void reg(int type, String name, int dataLength, FieldDef... fields) {
        TYPES.put(type, new TypeDef(type, name, dataLength, fields));
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }
}
