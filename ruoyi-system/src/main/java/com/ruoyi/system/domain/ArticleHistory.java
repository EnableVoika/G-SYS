package com.ruoyi.system.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.utils.uuid.IdUtils;

import java.util.Date;

public class ArticleHistory extends BaseEntity {

    /** id */
    private String id;

    /** 编号 */
    private String originalTableId;

    /** 档号 */
    private String archiveNo;

    /** 作者 */
    private String author;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** tag 名字 */
    private String tags;

    /** 来源 */
    private String source;

    /** 1=删除，0=未删除 */
    private int del;

    /** 文章状态 1=关闭，0=正常 */
    private Integer status;

    /** 创建者 */
    private String originalCreateBy;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date originalCreateTime;

    /** 更新者 */
    private String originalUpdateBy;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date originalUpdateTime;

    public ArticleHistory()
    {

    }

    public ArticleHistory(String tableId, String archiveNo, String author, String title, String content)
    {
        this.originalTableId = tableId;
        this.archiveNo = archiveNo;
        this.author = author;
        this.title = title;
        this.content = content;
    }

    public ArticleHistory(Article original)
    {
        this.id = IdUtils.fastSimpleUUID();
        this.originalTableId = original.getTableId();
        this.archiveNo = original.getArchiveNo();
        this.author = original.getAuthor();
        this.title = original.getTitle();
        this.content = original.getContent();
        this.tags = original.getTags();
        this.source = original.getSource();
        this.status = original.getStatus();
        this.originalCreateBy = original.getCreateBy();
        this.originalCreateTime = original.getCreateTime();
        this.originalUpdateBy = original.getUpdateBy();
        this.originalUpdateTime = original.getUpdateTime();
        this.setRemark(original.getRemark());
        this.setVersion(original.getVersion());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOriginalTableId() {
        return originalTableId;
    }

    public void setOriginalTableId(String originalTableId) {
        this.originalTableId = originalTableId;
    }

    public String getTableId()
    {
        return originalTableId;
    }

    public String getArchiveNo()
    {
        return archiveNo;
    }

    public String getTitle()
    {
        return title;
    }

    public String getContent()
    {
        return content;
    }

    public void setTableId(String tableId)
    {
        this.originalTableId = tableId;
    }

    public void setArchiveNo(String archiveNo)
    {
        this.archiveNo = archiveNo;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public int getDel() {
        return del;
    }

    public void setDel(int del) {
        this.del = del;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getOriginalCreateBy() {
        return originalCreateBy;
    }

    public void setOriginalCreateBy(String originalCreateBy) {
        this.originalCreateBy = originalCreateBy;
    }

    public Date getOriginalCreateTime() {
        return originalCreateTime;
    }

    public void setOriginalCreateTime(Date originalCreateTime) {
        this.originalCreateTime = originalCreateTime;
    }

    public String getOriginalUpdateBy() {
        return originalUpdateBy;
    }

    public void setOriginalUpdateBy(String originalUpdateBy) {
        this.originalUpdateBy = originalUpdateBy;
    }

    public Date getOriginalUpdateTime() {
        return originalUpdateTime;
    }

    public void setOriginalUpdateTime(Date originalUpdateTime) {
        this.originalUpdateTime = originalUpdateTime;
    }
}
