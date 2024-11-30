package com.ruoyi.system.service;

import com.ruoyi.system.domain.Article;
import org.springframework.ui.ModelMap;

import java.util.List;

public interface LifeAndLeisureServices {

    /**
     * 查询文章列表
     * @return
     */
    List<Article> search(Article condition);

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

    int edit_article(Article dto);

    int del_article(String _Id);

    int del_article_batch(List<String> _Ids);

}
