package com.ruoyi.system.service;

import com.ruoyi.system.domain.Article;

import java.util.List;

public interface LifeAndLeisureServices {

    /**
     * 查询文章列表
     * @return
     */
    List<Article> search();

    /**
     * 查询文章详情
     */
    Article find_article(String _Id);

    /**
     * 查询我的收藏文章
     * @param _UserId
     * @return
     */
    List<Article> find_favorite(long _UserId);

    int add_article(Article dto);

}
