package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.FileVO;
import com.ruoyi.common.core.domain.dto.FileDTO;

import java.util.List;

public interface FileService
{
    List<FileVO> list(String _RootPath, String relativePath);

    void mkdirs(String _RootPath, String relativePath);
}
