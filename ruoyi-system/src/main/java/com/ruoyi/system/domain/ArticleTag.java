package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;

public class ArticleTag extends BaseEntity {

    private String articleId;

    private String tag;

    public ArticleTag()
    {

    }

    public ArticleTag(String articleId, String tag)
    {
        this.articleId = articleId;
        this.tag = tag;
    }

    public String getArticleId() {
        return articleId;
    }

    public void setArticleId(String articleId) {
        this.articleId = articleId;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
