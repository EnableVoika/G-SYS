package com.ruoyi.system.service.impl;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.DelFailFile;
import com.ruoyi.common.core.domain.FileBO;
import com.ruoyi.common.core.domain.FileVO;
import com.ruoyi.common.core.domain.dto.RecycleInfoCondition;
import com.ruoyi.common.core.domain.dto.RecycleListDTO;
import com.ruoyi.common.core.domain.vo.system.filecontroller.RecycleVO;
import com.ruoyi.common.utils.bean.BeanUtils;
import com.ruoyi.system.domain.RecycleInfo;
import com.ruoyi.common.enums.ErrorCode;
import com.ruoyi.common.exception.ServiceExcept;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.ShiroUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileTypeUtils;
import com.ruoyi.common.utils.uuid.UUID;
import com.ruoyi.system.mapper.RecycleInfoMapper;
import com.ruoyi.system.service.FileService;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
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
    @Transactional
    public List<DelFailFile> recycle(long _UserId, List<String> _Paths) throws IOException
    {
        String userHome = getUserHome(_UserId);
        String deletedAt = DateUtils.dateTimeNow();
        List<DelFailFile> delFailList = new ArrayList<>();
        File userRecycleSpace = new File(recycleFolderRootPath, String.valueOf(_UserId));
        if (!userRecycleSpace.exists())
        {
            try
            {
                Files.createDirectories(userRecycleSpace.toPath());
            }
            catch(Exception e)
            {
                log.error("用户{userId={}}创建回收站失败, 异常原因: ", _UserId, e);
                throw new ServiceExcept("删除失败, 请联系管理员, 一个文件都没删除");
            }
        }
        // 把文件移动到用户专属回收站空间里
        for (String path : _Paths)
        {
            try
            {
                File originalFile = new File(userHome, path);
                if (!originalFile.exists())
                {
                    DelFailFile delFailFile = new DelFailFile(path, "文件不存在");
                    delFailList.add(delFailFile);
                    continue;
                }
                // 本次删除记录的uuid，同时也是文件夹名字
                String uuid =  UUID.randomUUID().toString();
                Path originalPath = originalFile.toPath();
                File uuidFilePath = new File(userRecycleSpace, uuid);
                // 创建本次删除记录的uuid文件夹
                Path uuidPath = Files.createDirectories(uuidFilePath.toPath());

                // 把文件准备写进uuid路径里
                File recycleFileRelativeFile = new File(uuidPath.toFile(), path);
                // 确保回收站路径中间目录存在
                Files.createDirectories(recycleFileRelativeFile.getParentFile().toPath());
                RecycleInfo recycleInfo = new RecycleInfo();
                if (originalFile.isDirectory())
                {
                    recycleInfo.setFileType(FileTypeUtils.getFileTypeIndex("DIR"));
                    recycleInfo.setTypeLabel(FileTypeUtils.getFileTypeIndexLabel(recycleInfo.getFileType()));
                }

                else if (originalFile.isFile())
                {
                    String suffix = FileTypeUtils.getFileType(originalFile.getName());
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
                recycleInfo.setUuid(uuid);
                recycleInfo.setUserId(_UserId);
                recycleInfo.setOriginalFileName(originalFile.getName());
                recycleInfo.setOriginalRelativePath(path);
                String osName = System.getProperty("os.name");
                if (osName.toLowerCase().startsWith("windows") || osName.toLowerCase().startsWith("win"))
                {
                    recycleInfo.setRecycleRelativePath(uuid + path);
                }
                else
                {
                    recycleInfo.setRecycleRelativePath(uuid + "/" + path);
                }
                recycleInfo.setDeletedAt(deletedAt);
                // 如果移动出现异常，直接走到catch，同时ql也不会插入，后续Files.move也不会执行
                recycleInfoMapper.insert(recycleInfo);
                Files.move(originalPath, recycleFileRelativeFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            catch (AccessDeniedException e)
            {
                log.error("{} 重命名失败, 目标正在被占用, 具体失败原因: ", path, e);
                DelFailFile delFailFile = new DelFailFile(path, "文件正在被占用, 删除失败");
                delFailList.add(delFailFile);
            }
            catch(Exception e)
            {
                log.error("{} 重命名失败, 失败原因: ", path, e);
                DelFailFile delFailFile = new DelFailFile(path, "文件删除失败");
                delFailList.add(delFailFile);
            }

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
            for (RecycleInfo recycleInfo : result)
            {
                RecycleVO recycleVO = new RecycleVO();
                BeanUtils.copyBeanProp(recycleVO, recycleInfo);
                data.add(recycleVO);
            }
        }
        return data;
    }

    public List<DelFailFile> permanentDels(Set<String> _UUIDs)
    {
        List<DelFailFile> delFailList = new ArrayList<>();
        Long userId = ShiroUtils.getUserId();
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
                throw new ServiceExcept("你无权删除其他人的文件");
            AtomicBoolean success = new AtomicBoolean(true);
            try
            {
                Path userRecycleSpacePath = Path.of(recycleFolderRootPath, String.valueOf(userId));
                Path recycleFileFullPath = Path.of(userRecycleSpacePath.toString(), uuid);
                // 递归删除目录及所有内容
                try (Stream<Path> stream = Files.walk(recycleFileFullPath))
                {
                    stream.sorted(Comparator.reverseOrder()).forEach(path ->
                    {
                        try
                        {
                            Files.delete(path);
                        } catch (IOException e)
                        {
                            success.set(false);
                            log.error("uuid={}, 路径={} 删除失败, 失败原因: ", uuid, path, e);
                            DelFailFile delFailFile = new DelFailFile(uuid, path + "删除失败, 失败原因: " + e.getMessage());
                            delFailList.add(delFailFile);
                        }
                    });
                }
            }
            catch (NoSuchFileException e)
            {
                success.set(false);
                log.error("{} 删除失败, 失败原因: {}", uuid, "没有找到该uuid对应的路径", e);
                DelFailFile delFailFile = new DelFailFile(uuid, "没有找到该文件, 该文件不在回收站或者路径错误");
                delFailList.add(delFailFile);
            }
            catch (Exception e)
            {
                success.set(false);
                log.error("{} 删除失败, 失败原因: ", uuid, e);
                DelFailFile delFailFile = new DelFailFile(uuid, e.getMessage());
                delFailList.add(delFailFile);
            }
            if (success.get())
            {
                try
                {
                    recycleInfoMapper.delByUUID(uuid);
                } catch (Exception e)
                {
                    log.error("{} 数据库记录删除失败, 失败原因: ", uuid, e);
                    delFailList.add(new DelFailFile(uuid, "数据库记录删除失败: " + e.getMessage()));
                }
            }
        }
        return delFailList;
    }
}
