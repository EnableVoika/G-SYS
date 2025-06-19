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
import com.ruoyi.common.utils.file.FileUtils;
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
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    private Path getUserRecycleSpace(long _UserId)
    {
        return Path.of(recycleFolderRootPath, String.valueOf(_UserId));
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
                topType = topType == -1 ? FileTypeUtils.getFileTypeIndex("FILE") : topType;
                topTypeLabel = FileTypeUtils.getFileTypeIndexLabel(topType);
            }

            // 获取_Group_OriginalRelativePath下的所有叶子路径
            FileUtils.collectLeafPaths(allSonPathStrs, _Original_Root, _Original_Relative);
            // group_uuid, 同时也是回收站对应的文件夹名字
            String groupUUID =  UUID.randomUUID().toString();
            // 展示回收站列表用的
            String groupName = topPath.getFileName().toString();
            // 用户专属回收站空间绝对地址
            Path userRecycleSpace = getUserRecycleSpace(_UserId);
            // 存放删除成功的记录
            List<RecycleInfo> insertData = new ArrayList<>();
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
                // 一个个插入效率有点慢
//                recycleInfoMapper.insert(recycleInfo);
                insertData.add(recycleInfo);
            }
            // 打总插入
            recycleInfoMapper.insert_batch(insertData);
            // 清空空目录
            FileUtils.cleanEmptyDirs(_DelFailList, topPath);
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
        List<String> delData = new ArrayList<>();
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
//                    recycleInfoMapper.delByUUID(recycleInfo.getUuid());
                    delData.add(recycleInfo.getUuid());
                }
                catch (IOException e)
                {
                    delFailList.add(new DelFailFile(recycleInfo.getOriginalRelativePath(), "删除失败"));
                }
            }
            recycleInfoMapper.delByUUIDs(delData);
            // 清空空目录
            try
            {
                FileUtils.cleanEmptyDirs(delFailList, userRecycleSpace.toString(), groupUUID);
            } catch (IOException e)
            {
                log.error("在清空空目录的时候失败, group_uuid={}, 具体异常信息: ", groupUUID, e);
                delFailList.add(new DelFailFile(groupUUID, "在清空空目录的时候失败"));
            }
        }
        return delFailList;
    }

    /**
     * 还原
     * 该函数不考虑部分还原失败问题，因为默认回收站里的文件是不允许被操作和查看的
     * 如果是本地系统占用、删除等一系列绕开G-SYS监控行为的，视为不可控因素
     * 否则默认情况文件能进入回收站，就说明文件已经在删除之前就解除了所有可以被使用的可能
     * 再加上回收站内不可操作文件，所以不可能出现 *部分还原失败* 情况
     * @param _UserId
     * @param _GroupUUIDs
     * @return
     */
    public List<DelFailFile> reverts(long _UserId, Set<String> _GroupUUIDs)
    {
        List<DelFailFile> delFailList = new ArrayList<>();
        Path userSpacePath = Path.of(getUserHome(_UserId));
        Path userRecycleSpacePath = getUserRecycleSpace(_UserId);
        List<String> delUuidData = new ArrayList<>();
        int count = 0;
        RecycleInfoCondition condition = new RecycleInfoCondition();
        condition.setUserId(_UserId);
        String datatimeStr = DateUtils.dateTimeNow();
        for (String groupUUID : _GroupUUIDs)
        {
            condition.setGroupUuid(groupUUID);
            List<RecycleInfo> searchResult = recycleInfoMapper.search(condition);
            if (CollectionUtils.isEmpty(searchResult))
            {
                continue;
            }
            datatimeStr = DateUtils.offsetSeconds(datatimeStr, count++, DateUtils.YYYYMMDDHHMMSS, DateUtils.YYYYMMDDHHMMSS);
            for (RecycleInfo recycleInfo : searchResult)
            {
                if (_UserId != Constants.ADMIN_USER_ID && !recycleInfo.getUserId().equals(_UserId))
                    throw new ServiceExcept("你无权操作其他人的文件");
                Path originalAbsolutPath = Path.of(userSpacePath.toString(), recycleInfo.getOriginalRelativePath());
                Path recycleAbsolutPath = Path.of(userRecycleSpacePath.toString(), recycleInfo.getRecycleRelativePath());
                // 如果用户空间没有这个文件，那就直接写入到原始位置
                if (!Files.exists(originalAbsolutPath))
                {
                    try
                    {
                        // 先创建父目录
                        Files.createDirectories(originalAbsolutPath.getParent());
                        Files.move(recycleAbsolutPath, originalAbsolutPath);
                        // 把移动成功的记录塞进list, 用于后面批量删除用
                        delUuidData.add(recycleInfo.getUuid());
                    }
                    catch (IOException e)
                    {
                        log.error("{} 还原失败, 异常详情: ", recycleInfo.getOriginalRelativePath(), e);
                        delFailList.add(new DelFailFile(recycleInfo.getOriginalRelativePath(), "还原失败, uuid=" + recycleInfo.getUuid()));
                    }

                }
                // 如果文件已经存在,判断当前目录是否是空目录，是的话，也写入原位置，否则就在用户空间新建一个目录，把文件还原到那里存放着
                else
                {
                    // 先判断原始路径是否是目录
                    if (Files.isDirectory(originalAbsolutPath))
                    {
                        // 判断是否是空目录
                        try
                        {
                            // 如果是空目录, 就直接跳过
                            if (FileUtils.isEmptyDir(originalAbsolutPath))
                            {
                                delUuidData.add(recycleInfo.getUuid());
                            }
                        }
                        catch (IOException e)
                        {
                            log.error("原始路径是一个空目录, 但 {} 依然还原失败, 异常详情: ", recycleInfo.getOriginalRelativePath(), e);
                            delFailList.add(new DelFailFile(recycleInfo.getOriginalRelativePath(), "原始路径是一个空目录, 但" + recycleInfo.getOriginalRelativePath() + "依然还原失败"));
                        }
                    }
                    // 如果不是目录, 而且还已经存在了, 就在用户空间根目录新建一个文件夹存放恢复的文件, 格式 恢复路径/日期/原始地址
                    else
                    {
                        // 设置恢复文件夹路径
                        Path revertAbsolutRootPath = Path.of(userSpacePath.toString(), Constants.REVERT_FOLDER_NAME);
                        // 设置原文件恢复到恢复文件夹的绝对地址
                        Path toRevertPath = Path.of(revertAbsolutRootPath.toString(), datatimeStr, recycleInfo.getOriginalRelativePath());
                        try
                        {
                            // 把父目录创建好
                            Files.createDirectories(toRevertPath.getParent());
                            // 开始移动文件
                            Files.move(recycleAbsolutPath, toRevertPath);
                            delUuidData.add(recycleInfo.getUuid());
                        }
                        catch (IOException e)
                        {
                            log.error("创建恢复文件夹失败, {} 无法恢复, 异常详情: ", recycleInfo.getOriginalRelativePath(), e);
                            delFailList.add(new DelFailFile(recycleInfo.getOriginalRelativePath(), "创建恢复文件夹失败, " + recycleInfo.getOriginalRelativePath() + "无法恢复"));
                        }
                    }
                }
            }
            // 清空回收站空目录
            try
            {
                FileUtils.cleanEmptyDirs(delFailList, userRecycleSpacePath.toString(), groupUUID);
            }
            catch (IOException e)
            {
                log.error("group_uuid={} 已经还原完成, 但是回收站里的残留空目录清理失败, 异常详情: ", groupUUID, e);
                delFailList.add(new DelFailFile(groupUUID, "group_uuid=" + groupUUID + "已经还原完成, 但是回收站里的残留空目录清理失败"));
            }
        }
        recycleInfoMapper.delByUUIDs(delUuidData);
        return delFailList;
    }






















//    public List<DelFailFile> reverts(Set<String> _UUIDs)
//    {
//        List<DelFailFile> delFailList = new ArrayList<>();
//        Long userId = ShiroUtils.getUserId();
//        String datatimeStr = DateUtils.dateTimeNow();
//        int count = 0;
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
//                throw new ServiceExcept("你无权操作其他人的文件");
//            Path userRecycleSpace = Path.of(recycleFolderRootPath, String.valueOf(userId));
//            String userHome = getUserHome(userId);
//            Path originalFileFullPath = Path.of(userHome, recycleInfo.getOriginalRelativePath());
//            List<String> recycleRelativePathStrs = new ArrayList<>();
//            try
//            {
//                Path recycleFileRootPath = Path.of(userRecycleSpace.toString(), recycleInfo.getUuid());
//                FileUtils.collectLeafPaths(recycleRelativePathStrs, recycleFileRootPath, recycleInfo.getOriginalRelativePath());
//            }
//            catch (NoSuchFileException e)
//            {
////                Path recycleFileFullPath = Path.of(userRecycleSpace.toString(), recycleInfo.getRecycleRelativePath());
//                log.error("获取叶子路径出现异常, 路径{} 不存在, 异常信息: ", e.getMessage(), e);
//                throw new ServiceExcept("还原失败");
//            }
//            catch (IOException e) {
//                log.error("获取叶子路径出现异常, 异常信息: ", e);
//                throw new ServiceExcept("还原失败");
//            }
//
//            for (String recycleRelativePathStr : recycleRelativePathStrs)
//            {
//                // 获取每一个叶子路径
//                Path recycleFullPath = Path.of(userRecycleSpace.toString(), recycleRelativePathStr);
//                // 现根据当前叶子路径，判断还原目标路径是否已经存在同名文件或文件夹
//                // 如果同名文件已经存在
//                if (Files.exists(originalFileFullPath))
//                {
//
//                }
//            }
//
//
//
//            // 如果文件已经存在，则创建一个“回收站恢复的文件”文件夹，里面用一个日期作为文件夹名、数字递增的文件夹储存恢复文件
//            if (Files.exists(originalFileFullPath))
//            {
//                Path moveTo = Path.of(userHome, Constants.REVERT_FOLDER_NAME, datatimeStr, String.valueOf(count++), recycleInfo.getOriginalRelativePath());
//                try
//                {
//                    Files.createDirectories(moveTo.getParent());
//                }
//                catch (SecurityException e)
//                {
//                    log.error("没有权限访问源文件目录, 异常信息: ", e);
//                    delFailList.add(new DelFailFile(uuid, "没有权限访问源文件目录"));
//                    continue;
//                }
//                catch (IOException e)
//                {
//                    log.error("创建存放恢复文件的文件夹失败, 失败原因: ", e);
//                    delFailList.add(new DelFailFile(uuid, "没有可用的位置存放恢复文件"));
//                    continue;
//                }
//                catch (RuntimeException e)
//                {
//                    log.error("恢复uuid={} 的文件失败, 失败原因: ", uuid, e);
//                    delFailList.add(new DelFailFile(uuid, "无法创建可用的位置存放恢复文件"));
//                    continue;
//                }
//                try
//                {
////                    Files.move(recycleFileFullPath, moveTo);
//                } catch (RuntimeException e)
//                {
//                    log.error("移动文件或文件夹失败, 失败原因: ", e);
//                    delFailList.add(new DelFailFile(uuid, "还原失败"));
//                    continue;
//                }
//                // delFailList.add(new DelFailFile(recycleInfo.getOriginalRelativePath(), "部分文件还原到了原位置，另一部分因为原位置存在重名文件，因此恢复到了“" + Constants.REVERT_FOLDER_NAME + "”当中"));
//            }
//            // 如果源地址没有重名路径，直接原样恢复，不创建恢复文件夹
//            else
//            {
//                try
//                {
//                    // 先创建父目录
//                    Files.createDirectories(originalFileFullPath.getParent());
//                }
//                catch (SecurityException e)
//                {
//                    log.error("没有权限访问源文件目录, 异常信息: ", e);
//                    delFailList.add(new DelFailFile(uuid, "没有权限访问源文件目录"));
//                    continue;
//                }
//                catch (IOException | RuntimeException e)
//                {
//                    log.error("移动文件或文件夹到源目录位置时失败, 失败原因: ", e);
//                    delFailList.add(new DelFailFile(uuid, "还原失败"));
//                    continue;
//                }
//                // 开始移动文件
//                try
//                {
////                    Files.move(recycleFileFullPath, originalFileFullPath);
//                } catch (RuntimeException e)
//                {
//                    log.error("移动文件或文件夹失败, 失败原因: ", e);
//                    delFailList.add(new DelFailFile(uuid, "还原失败"));
//                    continue;
//                }
//            }
//            // 全部成功移动，删除记录
//            recycleInfoMapper.delByUUID(uuid);
//        }
//        return delFailList;
//    }
}
