package com.zhian.gateway.third.others.mklora.constants;

/**
 * 铭控 LoRa 智能无线终端常量。
 */
public interface MkLoraConsts {

    String PLATFORM_CODE = "mkLora";
    String PROTOCOL_CODE = "mkLora";

    /** 终端设备类型编码 */
    String DEVICE_TYPE_CODE = "WFH";

    /** LoRa 网关设备类型编码 */
    String GATEWAY_TYPE_CODE = "LoraWLG";

    /** 网关设备编码前缀，完整编码 = 前缀 + 网关IP */
    String GATEWAY_CODE_PREFIX = "mklora-";

    /** 默认 TCP 监听端口 */
    int DEFAULT_PORT = 9211;

    /** 主动上报命令 */
    int CMD_REPORT = 0x10;

    /** 帧头 */
    int FRAME_HEADER = 0xA55A;

    /** 帧尾 */
    int FRAME_FOOTER = 0x55AA;

    /** 固定开销：头2+命令1+长度1+设备4+电量1+信号1+类型1+报警1+CRC2+尾2 */
    int FRAME_OVERHEAD = 16;

    int MIN_FRAME_LENGTH = FRAME_OVERHEAD;
    int MAX_FRAME_LENGTH = 255;
}
