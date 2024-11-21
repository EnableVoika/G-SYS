package com.ruoyi.system.service;

import com.ruoyi.system.domain.WriteMockBO;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface CommonFileServer {


    /**
     *
     * @param _F
     * @param argv 0是已经删除的文件个数 1是总文件个数
     */
    void delete(File _F,int argv[]);

    /**
     * 获取指定目录的文件
     * @param _Local
     * @return
     */
    List<String> list(String _Local);

    int write_mock(WriteMockBO bo) throws IOException;

    int write_mockf(WriteMockBO bo) throws IOException;

    String read_mock(String _MockId) throws IOException;

}
