package com.ruoyi.common.core.domain.dto;

import java.io.Serializable;

public class RecycleInfoCondition implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String uuid;

    private String groupUuid;

    private String groupName;

    private Long userId;

    private Integer fileType;

    private String typeLabel;

    private String originalFileName;

    private String originalRelativePath;

    private String recycleRelativePath;

    private String deletedAtBegin;

    private String deletedAtEnd;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
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

    public String getTypeLabel() {
        return typeLabel;
    }

    public void setTypeLabel(String typeLabel) {
        this.typeLabel = typeLabel;
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

    public String getDeletedAtBegin() {
        return deletedAtBegin;
    }

    public void setDeletedAtBegin(String deletedAtBegin) {
        this.deletedAtBegin = deletedAtBegin;
    }

    public String getDeletedAtEnd() {
        return deletedAtEnd;
    }

    public void setDeletedAtEnd(String deletedAtEnd) {
        this.deletedAtEnd = deletedAtEnd;
    }
}
