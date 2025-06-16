package com.ruoyi.common.core.domain;

public class FileTypeInfo
{
    private final int index;
    private final String label;

    public FileTypeInfo(int index, String label)
    {
        this.index = index;
        this.label = label;
    }

    public int getIndex()
    {
        return index;
    }

    public String getLabel()
    {
        return label;
    }


}
