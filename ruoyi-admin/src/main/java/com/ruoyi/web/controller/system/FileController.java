package com.ruoyi.web.controller.system;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.DelFailFile;
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
import java.util.ArrayList;
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
     *
     * @param _UserHome
     * @param _AccessPath
     * @param _Throw 是否抛出异常
     * @return
     * @throws IOException
     */
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
        String userHome = fileService.getUserHome(userId);
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
    @RequiresPermissions("system:file:mkdir")
    public AjaxResult mkdirs(FileDTO dto) throws IOException {
        if (StringUtils.isNotEmpty(dto.getPath()))
            dto.setPath((dto.getPath().startsWith("/") || dto.getPath().startsWith("\\")) ? dto.getPath() : ( "/" + dto.getPath()));
        Long userId = getUserId();
        String userHome =  fileService.getUserHome(userId);
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
    @DeleteMapping("/recycle")
    @ResponseBody
    @RequiresPermissions("system:file:recycle")
    public AjaxResult recycle(@RequestBody FileDTO dto) throws IOException
    {
        if (StringUtils.isEmpty(dto.getPath()) && (null ==  dto.getPaths() || dto.getPaths().isEmpty()))
            throw new ServiceExcept("文件路径不能为空");
        List<String> newPaths = new ArrayList<>();
        String userHome =  fileService.getUserHome();
        if (null != dto.getPaths())
        {
            for (String path : dto.getPaths())
            {
                if (StringUtils.isNotEmpty(path))
                {
                    path = (path.startsWith("/") || path.startsWith("\\")) ? path : ( "/" + path);
                    path = check_access_path(userHome, path, true);
                    newPaths.add(path);
                }
            }
        }
        if (StringUtils.isNotEmpty(dto.getPath()))
        {
            dto.setPath((dto.getPath().startsWith("/") || dto.getPath().startsWith("\\")) ? dto.getPath() : ( "/" + dto.getPath()));
            dto.setPath(check_access_path(userHome, dto.getPath(), true));
            newPaths.add(dto.getPath());
        }
        dto.setPaths(newPaths);
        List<DelFailFile> delFailFileList = fileService.recycle(getUserId(), dto.getPaths());
        if (!delFailFileList.isEmpty())
        {
            if (delFailFileList.size() == 1)
            {
                return AjaxResult.fail(delFailFileList.getFirst().getFailReason(), delFailFileList);
            }
            return AjaxResult.fail(ErrorCode.NOT_COMPLETELY_DELETED, "部分文件未能删除", delFailFileList);
        }
        return AjaxResult.ok("删除成功, 你可以到回收站里查看");
    }

}
