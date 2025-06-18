package com.ruoyi.common.core.domain.dto;

import java.util.List;

public class FileDTO
{
    private String uuid;

    private List<String> uuids;

    private String groupUuid;

    private List<String> groupUuids;

    private String path;

    private List<String> paths;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public List<String> getUuids() {
        return uuids;
    }

    public void setUuids(List<String> uuids) {
        this.uuids = uuids;
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }

    public List<String> getGroupUuids() {
        return groupUuids;
    }

    public void setGroupUuids(List<String> groupUuids) {
        this.groupUuids = groupUuids;
    }

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
