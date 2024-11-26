package com.ruoyi.system.domain;

public class TagRelation {

    /** 与该标签关联的表的唯一主键 */
    private String relationKey;

    /** 标签 */
    private String tag;

    /**
     * 来源
     * 1.生活娱乐-文章收录
     */
    private int source;

    public String getRelationKey() {
        return relationKey;
    }

    public void setRelationKey(String relationKey) {
        this.relationKey = relationKey;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }
}
