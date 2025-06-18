package com.ruoyi.system.service.impl;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.DelFailFile;
import com.ruoyi.common.core.domain.FileBO;
import com.ruoyi.common.core.domain.FileVO;
import com.ruoyi.common.core.domain.dto.RecycleInfoCondition;
import com.ruoyi.common.core.domain.dto.RecycleListDTO;
import com.ruoyi.common.core.domain.vo.system.filecontroller.RecycleVO;
import com.ruoyi.common.enums.ErrorCode;
import com.ruoyi.common.exception.ServiceExcept;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.ShiroUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.bean.BeanUtils;
import com.ruoyi.common.utils.file.FileTypeUtils;
import com.ruoyi.common.utils.uuid.UUID;
import com.ruoyi.system.domain.RecycleInfo;
import com.ruoyi.system.mapper.RecycleInfoMapper;
import com.ruoyi.system.service.FileService;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileServiceImpl implements FileService
{
    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);

    @Value("${ruoyi.default_file_path}")
    private String defaultFilePath;

    @Value("${ruoyi.recycle-folder-root-path}")
    private String recycleFolderRootPath;

    @Value("${ruoyi.custom-folder}")
    private String customFolderConfig;

    @Resource
    private RecycleInfoMapper recycleInfoMapper;

    /**
     * 递归收集root下所有叶子路径（相对root的路径）。
     * 所有相对路径保证无开头"/"或"\"，且分隔符全部为"/"。
     * @param resultList 输出结果（相对路径字符串，分隔符全为/）
     * @param root       根目录
     * @param relative   当前递归相对路径，可传"c/d"、"/c/d"、"\c\d"等任意风格
     */
    public static void collectLeafPaths(List<String> resultList, Path root, String relative) throws IOException
    {
        // 1. 规范化传入的相对路径
        String relNorm = relative.replaceAll("^[\\\\/]+", "")   // 去掉所有开头的斜杠
                .replace("\\", "/");           // 统一分隔符
        Path relPath = relNorm.isEmpty() ? Paths.get("") : Paths.get(relNorm);
        Path absPath = root.resolve(relPath);

        // 2. 判断叶子并收集
        if (Files.isRegularFile(absPath) || isEmptyDir(absPath)) {
            // 规范化list里所有路径
            String addPath = relPath.toString().replace("\\", "/");
            resultList.add(addPath);
            return;
        }

        // 3. 递归目录
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(absPath)) {
            for (Path child : stream) {
                // 递归时拼接新相对路径，并传递字符串格式
                String childRel = relNorm.isEmpty()
                        ? child.getFileName().toString()
                        : relNorm + "/" + child.getFileName().toString();
                collectLeafPaths(resultList, root, childRel);
            }
        }
    }

    public static void collectLeafPaths(List<String> _ResultList, String _Root, String _Relative) throws IOException {
        Path root = Path.of(_Root);
        collectLeafPaths(_ResultList, root, _Relative);
    }

    // 判断是不是空目录
    private static boolean isEmptyDir(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) return false;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            return !stream.iterator().hasNext();
        }
    }

    /**
     * 末尾不带 "/"
     * @param _Dest
     */
    public void getUserHome(StringBuffer _Dest)
    {
        long userId = ShiroUtils.getUserId();
        if (Constants.ADMIN_USER_ID.equals(userId))
        {
            _Dest.append(defaultFilePath);
            return;
        }
        _Dest.append(defaultFilePath).append("/").append(customFolderConfig).append("/").append(userId);
    }

    public String getUserHome()
    {
        long userId = ShiroUtils.getUserId();
        if (Constants.ADMIN_USER_ID.equals(userId))
            return defaultFilePath;
        return defaultFilePath + "/" + customFolderConfig + "/" + userId;
    }

    public String getUserHome(Long _UserId)
    {
        if (Constants.ADMIN_USER_ID.equals(_UserId))
            return defaultFilePath;
        return defaultFilePath + "/" + customFolderConfig + "/" + _UserId;
    }

    private void _list(List<FileBO> _Data, String _RootPath, String relativePath)
    {
        if (StringUtils.isEmpty(_RootPath))
            throw new ServiceExcept("根路径(_RootPath)不能为空");
        if (null == relativePath)
            relativePath = "";
        File directory = new File(_RootPath, relativePath);
        if (!directory.exists())
        {
            throw new ServiceExcept(ErrorCode.FILE_NOT_EXISTS,relativePath + "不存在");
        }
        if (directory.isFile())
        {
            throw new ServiceExcept(ErrorCode.NOT_A_DIR, "不是一个文件夹");
        }
        File[] files = directory.listFiles();
        if (files != null)
        {
            for (File file : files)
            {
                FileBO bo = new FileBO();
                bo.setName(file.getName());
                bo.setFullName(file.getAbsolutePath());
                bo.setLastPath(file.getParent());
                if (file.isDirectory())
                {
                    bo.setType(FileTypeUtils.getFileTypeIndex("DIR"));
                    bo.setShortName(file.getName());
                }
                else if (file.isFile())
                {
                    String suffixName = FileTypeUtils.getFileType(file.getName());
                    bo.setSuffix("." + suffixName);
                    bo.setSuffixName(suffixName);
                    bo.setShortName(file.getName().substring(0, file.getName().lastIndexOf(".")));
                    bo.setSize(file.length());
                    int fileTypeIndex = FileTypeUtils.getFileTypeIndex(suffixName);
                    bo.setType(fileTypeIndex == -1 ? FileTypeUtils.getFileTypeIndex("FILE") : fileTypeIndex);
                }
                _Data.add(bo);
            }
        }
    }

    /**
     * 列出文件列表
     * @param _RootPath
     * @param relativePath
     * @return
     */
    @Override
    public List<FileVO> list(String _RootPath, String relativePath)
    {
        List<FileBO> boList = new ArrayList<>();
        List<FileVO> data = new ArrayList<>();
        _list(boList, _RootPath, relativePath);
        for (FileBO fileBO : boList)
        {
            FileVO fileVO = new FileVO();
            fileVO.setName(fileBO.getName());
            fileVO.setShortName(fileBO.getShortName());
            fileVO.setSize(fileBO.getSize());
            fileVO.setType(fileBO.getType());
            fileVO.setSuffix(fileBO.getSuffix());
            fileVO.setSuffixName(fileBO.getSuffixName());
            String typeLabel = FileTypeUtils.getFileTypeIndexLabel(fileBO.getType());
            fileVO.setTypeLabel(StringUtils.isEmpty(typeLabel) ? "未知" : typeLabel);
            fileVO.setRelativePath(fileBO.getFullName().replace(_RootPath, ""));
            fileVO.setLastPath(fileBO.getLastPath().replace(_RootPath, ""));
            data.add(fileVO);
        }
        return data;
    }

    /**
     * 创建文件夹
     * @param _RootPath
     * @param relativePath
     */
    public void mkdirs(String _RootPath, String relativePath)
    {
        File folder = new File(_RootPath, relativePath);
        if (folder.exists())
        {
            String errMsg = "文件夹“";
            if (relativePath.startsWith("/") || relativePath.startsWith("\\"))
                relativePath = relativePath.substring(1);
            errMsg = errMsg + relativePath + "”已经存在";
            throw new ServiceExcept(ErrorCode.FILE_OR_DIR_HAS_EXISTS, errMsg);
        }
        if (!folder.mkdirs())
            throw new ServiceExcept(ErrorCode.ERROR, "文件夹创建失败");
    }


    /**
     * 移至回收站
     * 必须得重命名一个文件夹，写一个进list里，尽可能保证原子操作
     * @param _Paths
     */
//    @Transactional
//    public List<DelFailFile> recycle(long _UserId, List<String> _Paths) throws IOException
//    {
//        String userHome = getUserHome(_UserId);
//        String deletedAt = DateUtils.dateTimeNow();
//        List<DelFailFile> delFailList = new ArrayList<>();
//        File userRecycleSpace = new File(recycleFolderRootPath, String.valueOf(_UserId));
//        if (!userRecycleSpace.exists())
//        {
//            try
//            {
//                Files.createDirectories(userRecycleSpace.toPath());
//            }
//            catch(Exception e)
//            {
//                log.error("用户{userId={}}创建回收站失败, 异常原因: ", _UserId, e);
//                throw new ServiceExcept("删除失败, 请联系管理员, 一个文件都没删除");
//            }
//        }
//        // 把文件移动到用户专属回收站空间里
//        for (String path : _Paths)
//        {
//            try
//            {
//                File originalFile = new File(userHome, path);
//                if (!originalFile.exists())
//                {
//                    DelFailFile delFailFile = new DelFailFile(path, "文件不存在");
//                    delFailList.add(delFailFile);
//                    continue;
//                }
//                // 本次删除记录的uuid，同时也是文件夹名字
//                String uuid =  UUID.randomUUID().toString();
//                Path originalPath = originalFile.toPath();
//                File uuidFilePath = new File(userRecycleSpace, uuid);
//                // 创建本次删除记录的uuid文件夹
//                Path uuidPath = Files.createDirectories(uuidFilePath.toPath());
//
//                // 把文件准备写进uuid路径里
//                File recycleFileRelativeFile = new File(uuidPath.toFile(), path);
//                // 确保回收站路径中间目录存在
//                Files.createDirectories(recycleFileRelativeFile.getParentFile().toPath());
//                RecycleInfo recycleInfo = new RecycleInfo();
//                if (originalFile.isDirectory())
//                {
//                    recycleInfo.setFileType(FileTypeUtils.getFileTypeIndex("DIR"));
//                    recycleInfo.setTypeLabel(FileTypeUtils.getFileTypeIndexLabel(recycleInfo.getFileType()));
//                }
//
//                else if (originalFile.isFile())
//                {
//                    String suffix = FileTypeUtils.getFileType(originalFile.getName());
//                    int fileTypeIndex = FileTypeUtils.getFileTypeIndex(suffix);
//                    fileTypeIndex = fileTypeIndex == -1 ? FileTypeUtils.getFileTypeIndex("FILE") : fileTypeIndex;
//                    recycleInfo.setFileType(fileTypeIndex);
//                    recycleInfo.setTypeLabel(FileTypeUtils.getFileTypeIndexLabel(fileTypeIndex));
//                }
//                else
//                {
//                    int fileTypeIndex = FileTypeUtils.getFileTypeIndex("UNKNOW");
//                    recycleInfo.setFileType(fileTypeIndex);
//                    recycleInfo.setTypeLabel("未知");
//                }
//                recycleInfo.setUuid(uuid);
//                recycleInfo.setUserId(_UserId);
//                recycleInfo.setOriginalFileName(originalFile.getName());
//                recycleInfo.setOriginalRelativePath(path);
//                String osName = System.getProperty("os.name");
//                if (osName.toLowerCase().startsWith("windows") || osName.toLowerCase().startsWith("win"))
//                {
//                    recycleInfo.setRecycleRelativePath(uuid + path);
//                }
//                else
//                {
//                    recycleInfo.setRecycleRelativePath(uuid + "/" + path);
//                }
//                recycleInfo.setDeletedAt(deletedAt);
//                // 如果移动出现异常，直接走到catch，同时ql也不会插入，后续Files.move也不会执行
//                recycleInfoMapper.insert(recycleInfo);
//                Files.move(originalPath, recycleFileRelativeFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
//            }
//            catch (AccessDeniedException e)
//            {
//                log.error("{} 重命名失败, 目标正在被占用, 具体失败原因: ", path, e);
//                DelFailFile delFailFile = new DelFailFile(path, "文件正在被占用, 删除失败");
//                delFailList.add(delFailFile);
//            }
//            catch(Exception e)
//            {
//                log.error("{} 重命名失败, 失败原因: ", path, e);
//                DelFailFile delFailFile = new DelFailFile(path, "文件删除失败");
//                delFailList.add(delFailFile);
//            }
//
//        }
//        return delFailList;
//    }

    private Path getUserRecycleSpace(long _UserId)
    {
        return Path.of(recycleFolderRootPath, String.valueOf(_UserId));
    }

    public static void deleteEmptyDirsRecursively(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) return;
        // 递归处理所有子目录
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path child : stream) {
                deleteEmptyDirsRecursively(child);
            }
        }
        // 尝试删除本目录（只会删掉空目录）
        try {
            Files.delete(dir); // 只有空目录才能删掉，不会误删有内容的目录
        } catch (DirectoryNotEmptyException ignore) {
            // 有内容就跳过
        }
    }

    /**
     * 递归清理 rootDir 及其所有子目录下的空目录。
     * 如遇有文件，记录日志并跳过。
     * @param rootDir 待清理的目录
     * @return 返回未能删除的目录（目录下有文件）
     */
    public static List<Path> cleanEmptyDirs(List<DelFailFile> _DelFailList, Path rootDir) throws IOException {
        List<Path> undeletedDirs = new ArrayList<>();
        cleanEmptyDirsInternal(_DelFailList, rootDir, undeletedDirs);
        return undeletedDirs;
    }

    public static List<Path> cleanEmptyDirs(List<DelFailFile> _DelFailList, String _Root, String relative) throws IOException {
        Path fullPath = Path.of(_Root, relative);
        List<Path> undeletedDirs = new ArrayList<>();
        cleanEmptyDirsInternal(_DelFailList, fullPath, undeletedDirs);
        return undeletedDirs;
    }

    // 内部递归函数
    private static boolean cleanEmptyDirsInternal(List<DelFailFile> _DelFailList, Path dir, List<Path> undeletedDirs) throws IOException {
        if (!Files.isDirectory(dir))
            return false;
        boolean canDelete = true;

        // 检查子目录和文件
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path child : stream) {
                if (Files.isDirectory(child)) {
                    boolean subDirCanDelete = cleanEmptyDirsInternal(_DelFailList, child, undeletedDirs);
                    if (!subDirCanDelete) canDelete = false;
                } else {
                    // 有文件就不能删
                    canDelete = false;
                }
            }
        }

        if (canDelete) {
            try {
                Files.delete(dir);
                // System.out.println("删除空目录：" + dir);
            } catch (IOException e) {
                undeletedDirs.add(dir);
                // System.err.println("无法删除空目录 " + dir + "，异常：" + e.getMessage());
                return false;
            }
            return true;
        } else {
            undeletedDirs.add(dir); // 目录下还有文件
            // System.err.println("目录下还有文件，不能删除: " + dir);
            _DelFailList.add(new DelFailFile(dir.toString(), "还有文件，可能之前移动失败，因此该路径无法删除"));
            return false;
        }
    }


    private void _recycle(List<DelFailFile> _DelFailList, long _UserId, String _Original_Root, String _Original_Relative, String _DeletedAt) throws IOException {
        // 获取每一条源文件路径下面的所有叶子路径
        try
        {
            if (StringUtils.isEmpty(_DeletedAt))
            {
                _DeletedAt = DateUtils.dateTimeNow();
            }
            List<String> allSonPathStrs = new ArrayList<>();

            // 获取顶级父目录的文件类型
            Path topPath = Path.of(_Original_Root, _Original_Relative);
            int topType = -1;
            String topTypeLabel = "";
            if (Files.isDirectory(topPath))
            {
                topType = FileTypeUtils.getFileTypeIndex("DIR");
                topTypeLabel = FileTypeUtils.getFileTypeIndexLabel(topType);
            }
            else if(Files.isRegularFile(topPath))
            {
                topType = FileTypeUtils.getFileTypeIndex(FileTypeUtils.getFileType(topPath.toString()));
                topTypeLabel = FileTypeUtils.getFileTypeIndexLabel(topType);
            }

            // 获取_Group_OriginalRelativePath下的所有叶子路径
            collectLeafPaths(allSonPathStrs, _Original_Root, _Original_Relative);
            // group_uuid, 同时也是回收站对应的文件夹名字
            String groupUUID =  UUID.randomUUID().toString();
            // 展示回收站列表用的
            String groupName = topPath.getFileName().toString();
            // 用户专属回收站空间绝对地址
            Path userRecycleSpace = getUserRecycleSpace(_UserId);

            // 遍历每一个叶子节点
            for (String sonPathStr : allSonPathStrs)
            {
                // 本次删除记录的uuid
                String uuid =  UUID.randomUUID().toString();
                // 每一个叶子路径的绝对地址
                Path originalFullPath = Path.of(_Original_Root, sonPathStr);
                // 如果这个叶子路径不存在, 就返回给前端, 不插入数据库
                if (!Files.exists(originalFullPath))
                {
                    DelFailFile delFailFile = new DelFailFile(sonPathStr, "不存在, 因此无法删除");
                    _DelFailList.add(delFailFile);
                    continue;
                }
                RecycleInfo recycleInfo = new RecycleInfo();
                recycleInfo.setUuid(uuid);
                recycleInfo.setGroupUuid(groupUUID);
                recycleInfo.setGroupName(groupName);
                recycleInfo.setGroupRelativePath(_Original_Relative);
                if (Files.isDirectory(originalFullPath))
                {
                    recycleInfo.setFileType(FileTypeUtils.getFileTypeIndex("DIR"));
                    recycleInfo.setTypeLabel(FileTypeUtils.getFileTypeIndexLabel(recycleInfo.getFileType()));
                }
                else if (Files.isRegularFile(originalFullPath))
                {
                    String suffix = FileTypeUtils.getFileType(originalFullPath.getFileName().toString());
                    int fileTypeIndex = FileTypeUtils.getFileTypeIndex(suffix);
                    fileTypeIndex = fileTypeIndex == -1 ? FileTypeUtils.getFileTypeIndex("FILE") : fileTypeIndex;
                    recycleInfo.setFileType(fileTypeIndex);
                    recycleInfo.setTypeLabel(FileTypeUtils.getFileTypeIndexLabel(fileTypeIndex));
                }
                else
                {
                    int fileTypeIndex = FileTypeUtils.getFileTypeIndex("UNKNOW");
                    recycleInfo.setFileType(fileTypeIndex);
                    recycleInfo.setTypeLabel("未知");
                }
                recycleInfo.setUserId(_UserId);
                recycleInfo.setOriginalFileName(originalFullPath.getFileName().toString());
                recycleInfo.setOriginalRelativePath(sonPathStr);
                recycleInfo.setRelativeTopType(topType);
                recycleInfo.setRelativeTopTypeLabel(topTypeLabel);
                String osName = System.getProperty("os.name");
//                if (osName.toLowerCase().startsWith("windows") || osName.toLowerCase().startsWith("win"))
//                {
//                    recycleInfo.setRecycleRelativePath(uuid + "\\" + sonPathStr);
//                }
//                else
//                {
//                    recycleInfo.setRecycleRelativePath(uuid + "/" + sonPathStr);
//                }
                recycleInfo.setRecycleRelativePath(Path.of(groupUUID, sonPathStr).toString());
                recycleInfo.setDeletedAt(_DeletedAt);

                Path recycleUUIDFolder = Path.of(userRecycleSpace.toString(), groupUUID);

                Path recycleFullPath = Path.of(recycleUUIDFolder.toString(), sonPathStr);
                // 准备移动源文件到回收站
                try
                {
                    // 先为文件在回收站里创建父目录
                    Files.createDirectories(recycleFullPath.getParent());
                    // 确保父目录存在, 再移动文件到回收站
                    Files.move(originalFullPath, recycleFullPath);
                }
                catch (AccessDeniedException e)
                {
                    log.error("{} 移动失败, 目标正在被占用, 具体失败原因: ", sonPathStr, e);
                    DelFailFile delFailFile = new DelFailFile(sonPathStr, "文件正在被占用, 删除失败");
                    _DelFailList.add(delFailFile);
                    continue;
                }
                catch(Exception e)
                {
                    log.error("{} 移动失败, 失败原因: ", sonPathStr, e);
                    DelFailFile delFailFile = new DelFailFile(sonPathStr, "文件移至回收站失败");
                    _DelFailList.add(delFailFile);
                    continue;
                }
                recycleInfoMapper.insert(recycleInfo);
            }
            // 清空空目录
            cleanEmptyDirs(_DelFailList, topPath);
        }
        catch(Exception e)
        {
            log.error("userid={}, 路径={}, 移动文件失败, 失败原因: ", _UserId, _Original_Relative, e);
            DelFailFile delFailFile = new DelFailFile(_Original_Relative, "文件删除失败");
            _DelFailList.add(delFailFile);
        }
    }

    @Transactional
    public List<DelFailFile> recycle(long _UserId, List<String> _OriginalRelativePaths) throws IOException
    {
        String userHome = getUserHome(_UserId);
        List<DelFailFile> delFailList = new ArrayList<>();
        Path userRecycleSpace = getUserRecycleSpace(_UserId);
        // 如果用户的回收站空间不存在, 就创建一个
        try
        {
            Files.createDirectories(userRecycleSpace);
        }
        catch(Exception e)
        {
            log.error("用户{userId={}}创建回收站失败, 异常原因: ", _UserId, e);
            throw new ServiceExcept("删除失败, 请联系管理员, 一个文件都没删除");
        }
        String deletedAt = DateUtils.dateTimeNow();
        // 每一条源文件路径
        for (String originalRelativePath : _OriginalRelativePaths)
        {
            _recycle(delFailList, _UserId, userHome, originalRelativePath, deletedAt);
        }
        return delFailList;
    }

    public List<RecycleVO> recycleList(RecycleListDTO _Dto)
    {
        RecycleInfoCondition condition = new RecycleInfoCondition();
        BeanUtils.copyBeanProp(condition, _Dto);
        condition.setUserId(ShiroUtils.getUserId());
        List<RecycleInfo> result = recycleInfoMapper.search(condition);
        List<RecycleVO> data = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(result))
        {
            Map<String, List<RecycleInfo>> collect = result.stream().collect(Collectors.groupingBy(RecycleInfo::getGroupUuid));
            for (Map.Entry<String, List<RecycleInfo>> stringListEntry : collect.entrySet())
            {
                String groupUUID = stringListEntry.getKey();
                RecycleInfo recycleInfo = stringListEntry.getValue().getFirst();
                RecycleVO recycleVO = new RecycleVO();
                BeanUtils.copyBeanProp(recycleVO, recycleInfo);
                data.add(recycleVO);
            }
        }
        return data;
    }

    /**
     * 永久删除
     * @param _GroupUUIDs
     * @return
     */
    public List<DelFailFile> permanentDels(long _UserId, Set<String> _GroupUUIDs)
    {
        List<DelFailFile> delFailList = new ArrayList<>();
        RecycleInfoCondition condition = new RecycleInfoCondition();
        condition.setUserId(_UserId);
        Path userRecycleSpace = getUserRecycleSpace(_UserId);
        for (String groupUUID : _GroupUUIDs)
        {
            condition.setGroupUuid(groupUUID);
            // 查出这个userid下的所有属于这个group uuid的子路径
            List<RecycleInfo> searchResult = recycleInfoMapper.search(condition);
            if (CollectionUtils.isEmpty(searchResult))
            {
                delFailList.add(new DelFailFile(groupUUID, "回收站里不存在这个id的目录"));
                continue;
            }
            // 逐一删除文件和数据库记录
            for (RecycleInfo recycleInfo : searchResult)
            {
                try
                {
                    Files.delete(Path.of(userRecycleSpace.toString(), recycleInfo.getRecycleRelativePath()));
                    // 删除成功没有抛异常，就把数据库记录删除掉
                    recycleInfoMapper.delByUUID(recycleInfo.getUuid());
                }
                catch (IOException e)
                {
                    delFailList.add(new DelFailFile(recycleInfo.getOriginalRelativePath(), "删除失败"));
                }
            }
            // 清空空目录
            try
            {
                cleanEmptyDirs(delFailList, userRecycleSpace.toString(), groupUUID);
            } catch (IOException e)
            {
                log.error("在清空空目录的时候失败, group_uuid={}, 具体异常信息: ", groupUUID, e);
                delFailList.add(new DelFailFile(groupUUID, "在清空空目录的时候失败"));
            }
        }
        return delFailList;
    }

//    public List<DelFailFile> permanentDels(Set<String> _UUIDs)
//    {
//        List<DelFailFile> delFailList = new ArrayList<>();
//        Long userId = ShiroUtils.getUserId();
//        for (String uuid : _UUIDs)
//        {
//            RecycleInfo recycleInfo = recycleInfoMapper.findByUUID(uuid);
//            if (null == recycleInfo)
//            {
//                // 记录失败的uuid
//                DelFailFile delFailFile = new DelFailFile(uuid, "不存在");
//                delFailList.add(delFailFile);
//                continue;
//            }
//            if (!userId.equals(Constants.ADMIN_USER_ID) && !userId.equals(recycleInfo.getUserId()))
//                throw new ServiceExcept("你无权删除其他人的文件");
//            AtomicBoolean success = new AtomicBoolean(true);
//            try
//            {
//                Path userRecycleSpacePath = Path.of(recycleFolderRootPath, String.valueOf(userId));
//                Path recycleFileFullPath = Path.of(userRecycleSpacePath.toString(), uuid);
//                // 递归删除目录及所有内容
//                try (Stream<Path> stream = Files.walk(recycleFileFullPath))
//                {
//                    stream.sorted(Comparator.reverseOrder()).forEach(path ->
//                    {
//                        try
//                        {
//                            Files.delete(path);
//                        } catch (IOException e)
//                        {
//                            success.set(false);
//                            log.error("uuid={}, 路径={} 删除失败, 失败原因: ", uuid, path, e);
//                            DelFailFile delFailFile = new DelFailFile(uuid, path + "删除失败, 失败原因: " + e.getMessage());
//                            delFailList.add(delFailFile);
//                        }
//                    });
//                }
//            }
//            catch (NoSuchFileException e)
//            {
//                log.error("{} 删除失败, 失败原因: {}", uuid, "没有找到该uuid对应的路径", e);
//                DelFailFile delFailFile = new DelFailFile(uuid, "没有找到该文件, 该文件不在回收站或者路径错误");
//                delFailList.add(delFailFile);
//                continue;
//            }
//            catch (Exception e)
//            {
//                log.error("{} 删除失败, 失败原因: ", uuid, e);
//                DelFailFile delFailFile = new DelFailFile(uuid, e.getMessage());
//                delFailList.add(delFailFile);
//                continue;
//            }
//            if (success.get())
//            {
//                try
//                {
//                    recycleInfoMapper.delByUUID(uuid);
//                } catch (Exception e)
//                {
//                    log.error("{} 数据库记录删除失败, 失败原因: ", uuid, e);
//                    delFailList.add(new DelFailFile(uuid, "数据库记录删除失败: " + e.getMessage()));
//                }
//            }
//        }
//        return delFailList;
//    }

    /**
     * 还原
     * 该函数不考虑部分还原失败问题，因为默认回收站里的文件是不允许被操作和查看的
     * 如果是本地系统占用、删除等一系列绕开G-SYS监控行为的，视为不可控因素
     * 否则默认情况文件能进入回收站，就说明文件已经在删除之前就解除了所有可以被使用的可能
     * 再加上回收站内不可操作文件，所以不可能出现 *部分还原失败* 情况
     * @param _UUIDs
     * @return
     */
    public List<DelFailFile> reverts(Set<String> _UUIDs)
    {
        List<DelFailFile> delFailList = new ArrayList<>();
        Long userId = ShiroUtils.getUserId();
        String datatimeStr = DateUtils.dateTimeNow();
        int count = 0;
        for (String uuid : _UUIDs)
        {
            RecycleInfo recycleInfo = recycleInfoMapper.findByUUID(uuid);
            if (null == recycleInfo)
            {
                // 记录失败的uuid
                DelFailFile delFailFile = new DelFailFile(uuid, "不存在");
                delFailList.add(delFailFile);
                continue;
            }
            if (!userId.equals(Constants.ADMIN_USER_ID) && !userId.equals(recycleInfo.getUserId()))
                throw new ServiceExcept("你无权操作其他人的文件");
            Path userRecycleSpace = Path.of(recycleFolderRootPath, String.valueOf(userId));
            String userHome = getUserHome(userId);
            Path originalFileFullPath = Path.of(userHome, recycleInfo.getOriginalRelativePath());
            List<String> recycleRelativePathStrs = new ArrayList<>();
            try
            {
                Path recycleFileRootPath = Path.of(userRecycleSpace.toString(), recycleInfo.getUuid());
                collectLeafPaths(recycleRelativePathStrs, recycleFileRootPath, recycleInfo.getOriginalRelativePath());
            }
            catch (NoSuchFileException e)
            {
//                Path recycleFileFullPath = Path.of(userRecycleSpace.toString(), recycleInfo.getRecycleRelativePath());
                log.error("获取叶子路径出现异常, 路径{} 不存在, 异常信息: ", e.getMessage(), e);
                throw new ServiceExcept("还原失败");
            }
            catch (IOException e) {
                log.error("获取叶子路径出现异常, 异常信息: ", e);
                throw new ServiceExcept("还原失败");
            }

            for (String recycleRelativePathStr : recycleRelativePathStrs)
            {
                // 获取每一个叶子路径
                Path recycleFullPath = Path.of(userRecycleSpace.toString(), recycleRelativePathStr);
                // 现根据当前叶子路径，判断还原目标路径是否已经存在同名文件或文件夹
                // 如果同名文件已经存在
                if (Files.exists(originalFileFullPath))
                {

                }
            }



            // 如果文件已经存在，则创建一个“回收站恢复的文件”文件夹，里面用一个日期作为文件夹名、数字递增的文件夹储存恢复文件
            if (Files.exists(originalFileFullPath))
            {
                Path moveTo = Path.of(userHome, Constants.REVERT_FOLDER_NAME, datatimeStr, String.valueOf(count++), recycleInfo.getOriginalRelativePath());
                try
                {
                    Files.createDirectories(moveTo.getParent());
                }
                catch (SecurityException e)
                {
                    log.error("没有权限访问源文件目录, 异常信息: ", e);
                    delFailList.add(new DelFailFile(uuid, "没有权限访问源文件目录"));
                    continue;
                }
                catch (IOException e)
                {
                    log.error("创建存放恢复文件的文件夹失败, 失败原因: ", e);
                    delFailList.add(new DelFailFile(uuid, "没有可用的位置存放恢复文件"));
                    continue;
                }
                catch (RuntimeException e)
                {
                    log.error("恢复uuid={} 的文件失败, 失败原因: ", uuid, e);
                    delFailList.add(new DelFailFile(uuid, "无法创建可用的位置存放恢复文件"));
                    continue;
                }
                try
                {
//                    Files.move(recycleFileFullPath, moveTo);
                } catch (RuntimeException e)
                {
                    log.error("移动文件或文件夹失败, 失败原因: ", e);
                    delFailList.add(new DelFailFile(uuid, "还原失败"));
                    continue;
                }
                // delFailList.add(new DelFailFile(recycleInfo.getOriginalRelativePath(), "部分文件还原到了原位置，另一部分因为原位置存在重名文件，因此恢复到了“" + Constants.REVERT_FOLDER_NAME + "”当中"));
            }
            // 如果源地址没有重名路径，直接原样恢复，不创建恢复文件夹
            else
            {
                try
                {
                    // 先创建父目录
                    Files.createDirectories(originalFileFullPath.getParent());
                }
                catch (SecurityException e)
                {
                    log.error("没有权限访问源文件目录, 异常信息: ", e);
                    delFailList.add(new DelFailFile(uuid, "没有权限访问源文件目录"));
                    continue;
                }
                catch (IOException | RuntimeException e)
                {
                    log.error("移动文件或文件夹到源目录位置时失败, 失败原因: ", e);
                    delFailList.add(new DelFailFile(uuid, "还原失败"));
                    continue;
                }
                // 开始移动文件
                try
                {
//                    Files.move(recycleFileFullPath, originalFileFullPath);
                } catch (RuntimeException e)
                {
                    log.error("移动文件或文件夹失败, 失败原因: ", e);
                    delFailList.add(new DelFailFile(uuid, "还原失败"));
                    continue;
                }
            }
            // 全部成功移动，删除记录
            recycleInfoMapper.delByUUID(uuid);
        }
        return delFailList;
    }
}
