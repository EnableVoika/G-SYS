package com.ruoyi.system.domain;

import java.io.Serializable;

public class RecycleInfo implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String uuid;

    private String groupUuid;

    private String groupName;

    private String groupRelativePath;

    private Integer relativeTopType;

    private String relativeTopTypeLabel;

    private Long userId;

    private Integer fileType;

    private String typeLabel;

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

    public String getGroupRelativePath() {
        return groupRelativePath;
    }

    public void setGroupRelativePath(String groupRelativePath) {
        this.groupRelativePath = groupRelativePath;
    }

    public Integer getRelativeTopType() {
        return relativeTopType;
    }

    public void setRelativeTopType(Integer relativeTopType) {
        this.relativeTopType = relativeTopType;
    }

    public String getRelativeTopTypeLabel() {
        return relativeTopTypeLabel;
    }

    public void setRelativeTopTypeLabel(String relativeTopTypeLabel) {
        this.relativeTopTypeLabel = relativeTopTypeLabel;
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

    public String getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(String deletedAt) {
        this.deletedAt = deletedAt;
    }
}
