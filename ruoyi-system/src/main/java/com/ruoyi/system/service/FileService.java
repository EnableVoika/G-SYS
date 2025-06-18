package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.DelFailFile;
import com.ruoyi.common.core.domain.FileVO;
import com.ruoyi.common.core.domain.dto.FileDTO;
import com.ruoyi.common.core.domain.dto.RecycleListDTO;
import com.ruoyi.common.core.domain.vo.system.filecontroller.RecycleVO;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public interface FileService
{
    void getUserHome(StringBuffer _Dest);

    String getUserHome();

    String getUserHome(Long _UserId);

    List<FileVO> list(String _RootPath, String relativePath);

    void mkdirs(String _RootPath, String relativePath);

    List<DelFailFile> recycle(long _UserId, List<String> _OriginalRelativePaths) throws IOException;

    List<RecycleVO> recycleList(RecycleListDTO _Dto);

    List<DelFailFile> permanentDels(Set<String> _UUIDs);

    List<DelFailFile> reverts(Set<String> _UUIDs);
}
