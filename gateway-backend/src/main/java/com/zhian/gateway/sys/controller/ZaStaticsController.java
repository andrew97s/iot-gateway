package com.zhian.gateway.sys.controller;

import com.zhian.gateway.common.core.controller.BaseController;
import com.zhian.gateway.common.core.domain.AjaxResult;
import com.zhian.gateway.sys.domain.ZaSysDataset;
import com.zhian.gateway.sys.service.IZaSysDatasetService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

/**
 * 数据集Controller
 * 
 * @author yepanpan
 * @date 2024-04-11
 */
@Api("数据集")
@RestController
@RequestMapping("/sys/statics")
public class ZaStaticsController extends BaseController
{
    @Autowired
    private IZaSysDatasetService zaSysDatasetService;

    /**
     * 不分页查询数据集列表
     */
    @ApiOperation("不分页查询数据集列表")
    @GetMapping("/ds/{dsCode}")
    public AjaxResult ds(@PathVariable("dsCode") String dsCode, HttpServletRequest request, ZaSysDataset zaSysDataset)
    {
        zaSysDataset.setCode(dsCode);
        Enumeration<String> ps = request.getParameterNames();
        while(ps.hasMoreElements()){
            String p = ps.nextElement();
            zaSysDataset.getParams().put(p, request.getParameter(p));
        }
        return AjaxResult.success(zaSysDatasetService.query(zaSysDataset));
    }

}
