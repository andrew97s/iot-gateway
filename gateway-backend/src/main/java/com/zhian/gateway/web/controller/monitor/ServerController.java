package com.zhian.gateway.web.controller.monitor;

import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.framework.license.CustomLicenseService;
import com.zhian.gateway.framework.web.domain.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.zhian.gateway.common.core.domain.R.success;

/**
 * 服务器监控
 *
 * @author zhian
 */
@RestController
@RequestMapping("/monitor/server")
public class ServerController
{

    @Autowired
    private CustomLicenseService customLicenseService;

    @PreAuthorize("@ss.hasPermi('monitor:server:list')")
    @GetMapping()
    public R getInfo() throws Exception
    {
        Server server = new Server();
        server.copyTo();
        return success(server);
    }
}
