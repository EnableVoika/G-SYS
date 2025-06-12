package com.ruoyi.system.service.impl;

import com.ruoyi.common.core.domain.FileVO;
import com.ruoyi.common.core.domain.dto.FileDTO;
import com.ruoyi.common.enums.ErrorCode;
import com.ruoyi.common.exception.ServiceExcept;
import com.ruoyi.common.utils.StringUtils;
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

    @Override
    public List<FileVO> list(FileDTO dto)
    {
        String _RootPath = dto.getPath();
        if (StringUtils.isEmpty(_RootPath))
            throw new ServiceExcept("根路径(_RootPath)不能为空");
        File directory = new File(_RootPath);
        if (!directory.exists())
        {
            throw new ServiceExcept(ErrorCode.FILE_NOT_EXISTS,_RootPath + "不存在");
        }
        if (directory.isFile())
        {
            throw new ServiceExcept(ErrorCode.NOT_A_DIR, "不是一个文件夹");
        }
        String lastPath = "";
        if (_RootPath.equals(defaultFilePath))
            lastPath = defaultFilePath;
        else
            lastPath = new File(_RootPath).getParent();
        File[] files = directory.listFiles();
        List<FileVO> data = new ArrayList<>();
        if (files != null)
        {
            for (File file : files)
            {
                FileVO vo = new FileVO();
                vo.setName(file.getName());
                vo.setFullName(file.getPath());
//                vo.setPath(file.getPath());
                vo.setLastPath(lastPath);
                if (file.isDirectory())
                {
                    vo.setType(1);
                    vo.setShortName(file.getName());
                }
                else if (file.isFile())
                {
                    vo.setType(0);
                    String suffix = file.getName().substring(file.getName().lastIndexOf("."));
                    vo.setSuffix(suffix);
                    vo.setShortName(file.getName().substring(0, file.getName().lastIndexOf(".")));
                    vo.setSize(file.length());
                }
                vo.setCurrentPath(_RootPath);
                data.add(vo);
            }
        }
        return data;
    }
}
