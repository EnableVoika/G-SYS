package com.ruoyi.web.controller.system;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.FileVO;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.FileService;
import com.ruoyi.common.core.domain.dto.FileDTO;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Controller
@RequestMapping("/system/file")
public class FileController extends BaseController
{

    @Resource
    private FileService fileService;

    @Value("${ruoyi.default_file_path}")
    private String defaultFilePath;

    private String prefix = "system/file";

    /**
     * 获取文件列表页面
     */
    @RequiresPermissions("system:file:view")
    @GetMapping()
    public String article()
    {
        return prefix + "/view_list";
    }

    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(FileDTO dto)
    {
        if (StringUtils.isEmpty(dto.getPath()))
        {
            dto.setPath(defaultFilePath);
        }
        else
        {
            dto.setPath((dto.getPath().startsWith("/") || dto.getPath().startsWith("\\")) ? (defaultFilePath + dto.getPath()) : (defaultFilePath + "/" + dto.getPath()));
        }
        List<FileVO> data = fileService.list(dto);
        return getDataTable(data);
    }

}
