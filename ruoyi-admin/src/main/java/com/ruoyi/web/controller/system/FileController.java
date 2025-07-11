package com.ruoyi.web.controller.system;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.DelFailFile;
import com.ruoyi.common.core.domain.FileVO;
import com.ruoyi.common.core.domain.dto.FileDTO;
import com.ruoyi.common.core.domain.dto.RecycleListDTO;
import com.ruoyi.common.core.domain.vo.system.filecontroller.RecycleVO;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.ErrorCode;
import com.ruoyi.common.exception.ServiceExcept;
import com.ruoyi.common.utils.ShiroUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.FileService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

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

    private final String prefix = "system/file";

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
        long userId = getUserId();
        if (Constants.ADMIN_USER_ID.equals(userId))
        {
            return _AccessPath;
        }
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

    /**
     * 获取文件列表页面
     */
    @RequiresPermissions("system:file:view")
    @GetMapping()
    public String file_view()
    {
        return prefix + "/file_view";
    }

    /**
     * 获取回收站页面
     */
    @RequiresPermissions("system:file:view")
    @GetMapping("/recycle_view")
    public String recycle_view()
    {
        return prefix + "/recycle_view";
    }

    @RequiresPermissions("system:file:view")
    @PostMapping("/recycle_list")
    @ResponseBody
    public TableDataInfo recycle_list(RecycleListDTO _Dto)
    {
        List<RecycleVO> data = fileService.recycleList(_Dto);
        return getDataTable(data);
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
    public TableDataInfo file_view_list(FileDTO dto) throws IOException {
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

    @RequiresPermissions("system:file:remove")
    @DeleteMapping("/permanent")
    @ResponseBody
    public AjaxResult permanentDel(@RequestBody FileDTO _Dto)
    {
        Set<String> groupUUIDs = new HashSet<>();
        if (CollectionUtils.isNotEmpty(_Dto.getGroupUuids()))
            groupUUIDs.addAll(_Dto.getGroupUuids());
        if (StringUtils.isNotEmpty(_Dto.getUuid()))
            groupUUIDs.add(_Dto.getUuid());
        if (CollectionUtils.isEmpty(groupUUIDs))
            throw new ServiceExcept("文件路径id不能为空");
        long userId = getUserId();
        List<DelFailFile> delFailFiles = fileService.permanentDels(userId, groupUUIDs);
        if (CollectionUtils.isNotEmpty(delFailFiles))
        {
            if (delFailFiles.size() == groupUUIDs.size())
                return AjaxResult.fail(ErrorCode.NOT_COMPLETELY_DELETED, "文件删除失败", delFailFiles);
            return AjaxResult.fail(ErrorCode.NOT_COMPLETELY_DELETED, "部分文件删除失败", delFailFiles);
        }
        return AjaxResult.ok("文件删除成功");
    }

    /**
     * 回收站还原
     * @return
     */
    @RequiresPermissions("system:file:revert")
    @PostMapping("/revert")
    @ResponseBody
    public AjaxResult revert(@RequestBody FileDTO _Dto)
    {
        Set<String> groupUUIDs = new HashSet<>();
        if (CollectionUtils.isNotEmpty(_Dto.getGroupUuids()))
        {
            groupUUIDs.addAll(_Dto.getGroupUuids());
        }
        if (StringUtils.isNotEmpty(_Dto.getUuid()))
            groupUUIDs.add(_Dto.getUuid());
        if (CollectionUtils.isEmpty(groupUUIDs))
            throw new ServiceExcept("文件路径id不能为空");
        List<DelFailFile> delFailFiles = fileService.reverts(getUserId(), groupUUIDs);
        if (CollectionUtils.isNotEmpty(delFailFiles))
        {
            if (delFailFiles.size() == groupUUIDs.size())
                return AjaxResult.fail(ErrorCode.NOT_COMPLETELY_DELETED, "文件还原失败", delFailFiles);
            return AjaxResult.fail(ErrorCode.NOT_COMPLETELY_DELETED, "部分文件还原失败", delFailFiles);
        }
        return AjaxResult.ok("还原成功");
    }

    @RequiresPermissions("system:file:download")
    @GetMapping("/download")
    public ResponseEntity<?> simple_download(@RequestParam("path") String _Path, HttpServletRequest _Request) throws IOException {
        long userId = getUserId();
        String userHome = fileService.getUserHome(userId);
        Path resourceFullPath = Path.of(userHome, _Path);

        // 文件不存在
        if (!Files.exists(resourceFullPath)) {
            return ResponseEntity
                    .status(200)
                    .header("code", String.valueOf(ErrorCode.FILE_NOT_EXISTS.code()))
                    .header("msg", ErrorCode.FILE_NOT_EXISTS.what())
                    .body("文件不存在");
        }
        // 不是常规文件
        if (!Files.isRegularFile(resourceFullPath)) {
            return ResponseEntity
                    .status(200)
                    .header("code", String.valueOf(ErrorCode.NOT_A_REGULAR_FILE.code()))
                    .header("msg", ErrorCode.NOT_A_REGULAR_FILE.what())
                    .body("不是普通文件");
        }
        // 权限校验
        check_access_path(userHome, _Path, true);

        long size = Files.size(resourceFullPath);

        // 文件名编码
        String rawName = resourceFullPath.getFileName().toString();
        String encodeFileName = URLEncoder.encode(rawName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        String contentDisposition = "attachment; filename*=UTF-8''" + encodeFileName;

        InputStreamResource resource = new InputStreamResource(Files.newInputStream(resourceFullPath));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .header("code", "0")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(size)
                .body(resource);
    }

    @RequiresPermissions("system:file:upload")
    @PostMapping("/upload")
    @ResponseBody
    public AjaxResult simple_upload(@RequestParam("path") String path, @RequestParam("file") MultipartFile multipartFile) throws IOException {
        if (StringUtils.isNotEmpty(path))
            path = (path.startsWith("/") || path.startsWith("\\")) ? path : ( "/" + path);
        long userId = getUserId();
        String userHome = fileService.getUserHome(userId);
        String decodePath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        decodePath = check_access_path(userHome, decodePath, true);
        // 保存位置
        Path savePath = Path.of(userHome, decodePath, Objects.requireNonNull(multipartFile.getOriginalFilename()));
        try (InputStream is = multipartFile.getInputStream())
        {
            Files.copy(is, savePath, StandardCopyOption.REPLACE_EXISTING);
        }
        return AjaxResult.ok("上传成功");
    }

    @RequiresPermissions("system:file:upload")
    @GetMapping("/upload")
    public String simple_upload_view(@RequestParam("path") String path, Model model) {
        if (StringUtils.isNotEmpty(path))
            path = (path.startsWith("/") || path.startsWith("\\")) ? path : ( "/" + path);
        String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        model.addAttribute("path", encodedPath);
        return prefix + "/upload";
    }



}
