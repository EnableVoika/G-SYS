package com.ruoyi.common.core.domain;

public class FileVO
{
    // 包含后缀的名字
    private String name;

    // 不包含后缀的文件名
    private String shortName;

    // 相对路径
    private String relativePath;

    // 上一级
    private String lastPath;

    // 当前目录
//    private String currentDirectory;

    // 带.后缀
    private String suffix;

    // 不带.的后缀
    private String suffixName;

    // 文件大小
    private long size;

    // 权限
//    private String permission;

    // 文件还是文件夹 0=文件 1=文件夹
    private int type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getLastPath() {
        return lastPath;
    }

    public void setLastPath(String lastPath) {
        this.lastPath = lastPath;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

//    public String getPermission() {
//        return permission;
//    }
//
//    public void setPermission(String permission) {
//        this.permission = permission;
//    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public String getSuffixName() {
        return suffixName;
    }

    public void setSuffixName(String suffixName) {
        this.suffixName = suffixName;
    }
}
