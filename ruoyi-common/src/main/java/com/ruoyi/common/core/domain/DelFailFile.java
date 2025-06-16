package com.ruoyi.common.core.domain;

public class DelFailFile
{
    private String path;

    private String failReason;

    public DelFailFile()
    {
    }

    public DelFailFile(String path, String failReason)
    {
        this.path = path;
        this.failReason = failReason;
    }

    public String getFailReason()
    {
        return failReason;
    }

    public void setFailReason(String failReason)
    {
        this.failReason = failReason;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }
}
