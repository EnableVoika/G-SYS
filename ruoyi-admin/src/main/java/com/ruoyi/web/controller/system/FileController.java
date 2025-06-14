package com.ruoyi.web.controller.system;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.FileVO;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.ErrorCode;
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
     * 末尾不带 "/"
     * @param _Dest
     */
    private void getUserHome(StringBuffer _Dest)
    {
        long userId = getUserId();
        if (Constants.ADMIN_USER_ID.equals(userId))
        {
            _Dest.append(defaultFilePath);
            return;
        }
        _Dest.append(defaultFilePath).append("/").append(customFolderConfig).append("/").append(userId);
    }

    private String getUserHome()
    {
        long userId = getUserId();
        if (Constants.ADMIN_USER_ID.equals(userId))
            return defaultFilePath;
        return defaultFilePath + "/" + customFolderConfig + "/" + userId;
    }

    private String getUserHome(Long _UserId)
    {
        if (Constants.ADMIN_USER_ID.equals(_UserId))
            return defaultFilePath;
        return defaultFilePath + "/" + customFolderConfig + "/" + _UserId;
    }

    private String check_access_path(String _UserHome, String _AccessPath, boolean _Throw) throws IOException {
        long userId = getUserId();
        if (Constants.ADMIN_USER_ID.equals(userId))
        {
            return _AccessPath;
        }
        File userSpace = new File(_UserHome);
        File baseDir = userSpace.getAbsoluteFile(); // 你的根路径
        File userToAccessFile = new File(baseDir, _AccessPath == null ? "" : _AccessPath);
        String canonicalBase = baseDir.getCanonicalPath();
        String canonicalUserFile = userToAccessFile.getCanonicalPath();
        if (!canonicalUserFile.startsWith(canonicalBase))
        {
            if (_Throw)
                throw new ServiceExcept("非法访问");
            // 目录越界，强行拉回用户根目录
            return "";
        }
        return _AccessPath;
    }
    private String check_access_path(File userSpace, String _AccessPath, boolean _Throw) throws IOException
    {
        return check_access_path(userSpace.getCanonicalPath(), _AccessPath, _Throw);
    }


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
        String userHome = getUserHome(userId);
        // admin不走用户逻辑
        if (Constants.ADMIN_USER_ID.equals(userId))
        {
            List<FileVO> data = fileService.list(userHome, dto.getPath());
            return getDataTable(data);
        }
        // 如果是非管理员进来，那么只允许他们看各自的空间
//        String customFolderPath = defaultFilePath + "/" + customFolderConfig;
        File userSpace = new File(userHome);
        if (!userSpace.exists())
        {
            if (!userSpace.mkdirs())
                throw new ServiceExcept("创建用户空间失败");
            List<FileVO> data = fileService.list(userSpace.getAbsolutePath(), "");
            return getDataTable(data, ErrorCode.SUCCESS.what());
        }

        dto.setPath(check_access_path(userSpace, dto.getPath(), false));
        List<FileVO> data = fileService.list(userSpace.getAbsolutePath(), dto.getPath());
        return getDataTable(data, ErrorCode.SUCCESS.what());
    }

    @PostMapping("/mkdirs")
    @ResponseBody
    @RequiresPermissions("system:file:add")
    public AjaxResult mkdirs(FileDTO dto) throws IOException {
        if (StringUtils.isNotEmpty(dto.getPath()))
            dto.setPath((dto.getPath().startsWith("/") || dto.getPath().startsWith("\\")) ? dto.getPath() : ( "/" + dto.getPath()));
        Long userId = getUserId();
        String userHome = getUserHome(userId);
        if (Constants.ADMIN_USER_ID.equals(userId))
        {
            fileService.mkdirs(userHome, dto.getPath());
            return AjaxResult.ok("创建成功");
        }
        String accessPath = check_access_path(userHome, dto.getPath(), true);
        fileService.mkdirs(userHome, accessPath);
        return AjaxResult.ok("创建成功");
    }

    /**
     * 这个接口只会让文件移动到回收站
     * 先给用户创建回收站(Trash)
     * @param dto
     * @return
     */
    @DeleteMapping("/remove")
    public AjaxResult remove(FileDTO dto)
    {
        return AjaxResult.ok();
    }

}
