package com.ruoyi.web.controller.system;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.FileVO;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceExcept;
import com.ruoyi.common.utils.ShiroUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.FileService;
import com.ruoyi.common.core.domain.dto.FileDTO;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/system/file")
public class FileController extends BaseController
{

    @Resource
    private FileService fileService;

    @Value("${ruoyi.default_file_path}")
    private String defaultFilePath;

    @Value("${ruoyi.custom-folder}")
    private String customFolderConfig;

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

    /**
     * 这个接口非常危险，它能操作这个项目的所有文件
     * 除非管理员，否则建议使用用户空间接口
     * @param dto
     * @return
     */
//    @RequiresPermissions("system:file:admin_view")
//    @PostMapping("/list")
//    @ResponseBody
//    public TableDataInfo admin_view_list(FileDTO dto)
//    {
//        if (StringUtils.isNotEmpty(dto.getPath()))
//            dto.setPath((dto.getPath().startsWith("/") || dto.getPath().startsWith("\\")) ? dto.getPath() : ( "/" + dto.getPath()));
//        List<FileVO> data = fileService.list(defaultFilePath, dto.getPath());
//        return getDataTable(data);
//    }

    /**
     * 获取文件列表
     * @param dto
     * @return
     */
    @RequiresPermissions("system:file:view")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo user_view_list(FileDTO dto) throws IOException {
        if (StringUtils.isNotEmpty(dto.getPath()))
            dto.setPath((dto.getPath().startsWith("/") || dto.getPath().startsWith("\\")) ? dto.getPath() : ( "/" + dto.getPath()));
        long userId = ShiroUtils.getUserId();
        // admin不走用户逻辑
        if (1 == userId)
        {
            List<FileVO> data = fileService.list(defaultFilePath, dto.getPath());
            return getDataTable(data);
        }
        // 如果是非管理员进来，那么只允许他们看各自的空间
        String relativeCustomFolder = defaultFilePath + "/" + customFolderConfig;
        File userSpace = new File(relativeCustomFolder, String.valueOf(userId));
        if (!userSpace.exists())
        {
            if (!userSpace.mkdirs())
                throw new ServiceExcept("创建用户空间失败");
        }

        File baseDir = userSpace.getAbsoluteFile(); // 你的根路径
        File userToAccessFile = new File(baseDir, dto.getPath() == null ? "" : dto.getPath());
        String canonicalBase = baseDir.getCanonicalPath();
        String canonicalUserFile = userToAccessFile.getCanonicalPath();
        if (!canonicalUserFile.startsWith(canonicalBase)) {
            // 目录越界，强行拉回用户根目录
            dto.setPath("");
        }

        List<FileVO> data = fileService.list(userSpace.getAbsolutePath(), dto.getPath());
        return getDataTable(data);
    }

}
