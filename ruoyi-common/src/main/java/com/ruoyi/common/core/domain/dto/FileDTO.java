package com.ruoyi.common.core.domain.dto;

import java.util.List;

public class FileDTO
{
    private String path;

    private List<String> paths;

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }
}
