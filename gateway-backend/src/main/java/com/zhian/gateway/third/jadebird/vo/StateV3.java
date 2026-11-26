package com.zhian.gateway.third.jadebird.vo;

import lombok.Data;

/**
 * @author luping
 * Description: 青鸟云V3状态
 * @version 1.0
 * @date 2024/11/28 15:46
 */
@Data
public class StateV3 {
    //是否为恢复/撤销，0:发生；1：恢复
    private Integer recover;
    //事件类型，参考数据字典state_type
    private Integer stateTypeCode;
    //事件，参考数据字典state
    private Integer stateCode;


}
