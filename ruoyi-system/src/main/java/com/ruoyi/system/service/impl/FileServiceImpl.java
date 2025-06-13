package com.ruoyi.system.service.impl;

import com.ruoyi.common.core.domain.FileBO;
import com.ruoyi.common.core.domain.FileVO;
import com.ruoyi.common.core.domain.dto.FileDTO;
import com.ruoyi.common.enums.ErrorCode;
import com.ruoyi.common.exception.ServiceExcept;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileTypeUtils;
import com.ruoyi.system.service.FileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileServiceImpl implements FileService
{
    @Value("${ruoyi.default_file_path}")
    private String defaultFilePath;

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

    private void setFileType(FileBO _FileBO)
    {

    }

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

            fileVO.setRelativePath(fileBO.getFullName().replace(_RootPath, ""));
            fileVO.setLastPath(fileBO.getLastPath().replace(_RootPath, ""));
            data.add(fileVO);
        }
        return data;
    }
}
