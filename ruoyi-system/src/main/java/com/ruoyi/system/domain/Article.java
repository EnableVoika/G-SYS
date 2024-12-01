package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;

public class Article extends BaseEntity {

    /** 编号 */
    private String tableId;

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

    /**
     * r18标记
     * 1=r19
     * 0=正常
     */
    private Integer r18;

    public Article()
    {

    }

    public String getTableId()
    {
        return tableId;
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
        this.tableId = tableId;
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

    public Integer getR18() {
        return r18;
    }

    public void setR18(Integer r18) {
        this.r18 = r18;
    }
}
