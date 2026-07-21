package com.zhian.gateway.core.message;

import com.zhian.gateway.sys.domain.ZaMonitorType;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
    public static class Telemetry {

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
         * 监测值代码{@link ZaMonitorType#getUnit()}
         */
        private String unit;

        /**
         * 监测值时间
         */
        private long timestamp;
    }
}
