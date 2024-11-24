package com.ruoyi.web.controller.domain.vo;

public class DocumentModelVO {

    /** 编号 */
    private int tableId;

    /** 档号 */
    private String archiveNo;

    /** 文件编号 */
    private String docNo;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    public DocumentModelVO()
    {

    }

    public DocumentModelVO(int tableId, String archiveNo, String docNo, String title, String content)
    {
        this.tableId = tableId;
        this.archiveNo = archiveNo;
        this.docNo = docNo;
        this.title = title;
        this.content = content;
    }

    public int getTableId()
    {
        return tableId;
    }

    public String getArchiveNo()
    {
        return archiveNo;
    }

    public String getDocNo()
    {
        return docNo;
    }

    public String getTitle()
    {
        return title;
    }

    public String getContent()
    {
        return content;
    }

    public void setTableId(int tableId)
    {
        this.tableId = tableId;
    }

    public void setArchiveNo(String archiveNo)
    {
        this.archiveNo = archiveNo;
    }

    public void setDocNo(String docNo)
    {
        this.docNo = docNo;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

}
