package com.zhian.gateway.third.jadebird.vo;

import lombok.Data;

/**
 * @author luping
 * Description: 设备状态变化产生的事件
 * @version 1.0
 * @date 2024/11/28 10:26
 */
@Data
public class JaderBirdEventV3 {

    //探测器节点编号，默认
    private Integer nodeNo;

    //是否为恢复/撤销，0:发生；1：恢复
    private Integer recover;

    //事件类型，参考数据字典state_type
    private Integer stateTypeCode;

    //事件，参考数据字典state
    private Integer stateCode;
    //事件名称
    private String stateName;


}
