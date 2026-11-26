package com.zhian.gateway.third.jadebird.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Description: 青鸟云v3接口
 *
 * @date 2024/11/27 17:23
 * @Param
 * @return
 */
@Data
public class JadeBirdMsgV3 implements Serializable {

    private Integer cmd;
    private String echo;
    private Integer objectClassCode;

    //设备信息
    private JaderBirdFacilityV3 facility;

    //事件列表
    private List<JaderBirdEventV3> eventList;


    public enum Cmd {
        /**
         * 1：新增数据项；
         **/
        INSERT(1),
        /**
         * 2：删除数据项；
         **/
        DELETE(2),
        /**
         * 3：修改数据项；
         **/
        UPDATE(3),
        /**
         * 接口有效性测试
         */
        TEST_API(100),
        /**
         * 设备状态变更
         */
        DEVICE_STATUS_CHANGE(101),
        /**
         * 处警
         */
        ALARM_HANDLING(102);
        private int code;

        Cmd(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }


}
