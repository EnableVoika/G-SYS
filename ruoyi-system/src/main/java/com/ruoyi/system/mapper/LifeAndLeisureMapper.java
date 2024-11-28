package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.Article;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface LifeAndLeisureMapper {

    /**
     * 添加文章
     * @param po
     * @return
     */
    int add_article(Article po);

    int edit_article(Article po);

    int del_article(String id);

    Article find_article(String id);

    List<Article> search();

    /**
     * 批量查询文章列表
     * @return
     */
    List<Article> search_batch(@Param("_ArticleIds") List<String> _ArticleIds);

}
