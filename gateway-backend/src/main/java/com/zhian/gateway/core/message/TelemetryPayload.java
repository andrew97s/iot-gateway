package com.zhian.gateway.core.message;

import com.zhian.gateway.sys.domain.ZaMonitorType;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;

/**
 * 负载-监测数据
 *
 * @author tongwenjin
 * @since 2026/7/20
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class TelemetryPayload extends MessagePayload {

    private List<Telemetry> telemetries;

    /**
     * The type Telemetry.
     */
    @Data
    @Builder
    public static class Telemetry {

        /**
         * 监测值的通道，设备可能存在多个监测类型，每个类型对应一个唯一的通道号
         */
        private int channel = 1;

        /**
         * 监测值代码{@link ZaMonitorType#getCode()}
         */
        private String code;

        /**
         * 监测值代码{@link ZaMonitorType#getName()}
         */
        private String name;

        /**
         * 监测值
         */
        private String value;

        /**
         * 监测值低阈值
         */
        private String thresholdLow;

        /**
         * 监测值高阈值
         */
        private String thresholdHigh;

        /**
         * 监测值代码{@link ZaMonitorType#getUnit()}
         */
        private String unit;

        /**
         * 监测值时间
         */
        private long timestamp;

        /**
         * 监测值描述
         */
        private String desc;
    }
}
