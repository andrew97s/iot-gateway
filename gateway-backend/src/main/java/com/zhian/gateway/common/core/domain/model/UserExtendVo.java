package com.zhian.gateway.common.core.domain.model;

import com.zhian.gateway.common.annotation.Excel;
import com.zhian.gateway.common.core.domain.BaseEntity;
import lombok.Data;

/**
 * 人员基本信息档案对象VO
 *
 * @author yangyixin
 * @date 2023-04-20
 */
@Data
public class UserExtendVo extends BaseEntity {
    /**
     * 姓名
     */
    @Excel(name = "姓名")
    private String nickName;

    /**
     * 部门名称
     */
    @Excel(name = "所属部门")
    private String deptName;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 性别
     */
    @Excel(name = "性别", dictType = "sys_user_sex", type = Excel.Type.EXPORT)
    private String sex;

    /**
     * 照片
     */
    private String photo;

}
