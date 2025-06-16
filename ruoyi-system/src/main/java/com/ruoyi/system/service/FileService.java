package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.DelFailFile;
import com.ruoyi.common.core.domain.FileVO;
import com.ruoyi.common.core.domain.dto.FileDTO;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public interface FileService
{
    void getUserHome(StringBuffer _Dest);

    String getUserHome();

    String getUserHome(Long _UserId);

    List<FileVO> list(String _RootPath, String relativePath);

    void mkdirs(String _RootPath, String relativePath);

    List<DelFailFile> recycle(long _UserId, List<String> _Paths) throws IOException;
}
