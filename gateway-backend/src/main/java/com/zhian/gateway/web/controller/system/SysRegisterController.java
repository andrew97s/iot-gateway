package com.zhian.gateway.web.controller.system;

import com.zhian.gateway.common.core.controller.BaseController;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.core.domain.model.RegisterBody;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.system.service.ISysConfigService;
import com.zhian.gateway.system.service.impl.SysRegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 注册验证
 * 
 * @author zhian
 */
@RestController
public class SysRegisterController extends BaseController
{
    @Autowired
    private SysRegisterService registerService;

    @Autowired
    private ISysConfigService configService;

    @PostMapping("/register")
    public R register(@RequestBody RegisterBody user)
    {
        if (!("true".equals(configService.selectConfigByKey("sys.account.registerUser"))))
        {
            return error("当前系统没有开启注册功能！");
        }
        String msg = registerService.register(user);
        return StringUtils.isEmpty(msg) ? success() : error(msg);
    }
}
