package com.ruoyi.web.controller.demo.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RequestMapping("/demo/cpp-api")
@RestController
public class DemoCppAPIController {

    /**
     * 返回成功请求的demo接口
     * 用于查看成功请求的数据格式
     * @return
     */
    @GetMapping("/success")
    public AjaxResult demo_success(@RequestParam(required = false) Map<String,Object> params)
    {
        if (params.isEmpty())
            params = null;
        return AjaxResult.success("请求成功",params);
    }

    /**
     * 返回成功请求的demo接口
     * 用于查看成功请求的数据格式
     * @return
     */
    @GetMapping("/fail")
    public AjaxResult demo_fail(@RequestParam(required = false) Map<String,Object> params)
    {
        if (params.isEmpty())
            params = null;
        return AjaxResult.error("请求失败",params);
    }

}
