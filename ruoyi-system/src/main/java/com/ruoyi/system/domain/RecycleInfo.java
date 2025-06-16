package com.ruoyi.system.domain;

import java.io.Serializable;

public class RecycleInfo implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String uuid;

    private Long userId;

    private Integer fileType;

    private String originalFileName;

    private String originalRelativePath;

    private String recycleRelativePath;

    private String deletedAt;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getFileType() {
        return fileType;
    }

    public void setFileType(Integer fileType) {
        this.fileType = fileType;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getOriginalRelativePath() {
        return originalRelativePath;
    }

    public void setOriginalRelativePath(String originalRelativePath) {
        this.originalRelativePath = originalRelativePath;
    }

    public String getRecycleRelativePath() {
        return recycleRelativePath;
    }

    public void setRecycleRelativePath(String recycleRelativePath) {
        this.recycleRelativePath = recycleRelativePath;
    }

    public String getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(String deletedAt) {
        this.deletedAt = deletedAt;
    }
}
