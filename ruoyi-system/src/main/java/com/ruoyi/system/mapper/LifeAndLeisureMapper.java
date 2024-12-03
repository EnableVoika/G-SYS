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

    int save_article(Article po);

    int del_article(@Param("id") String _Id);

    int del_article_batch(@Param("ids") List<String> _Ids);

    Article find_article(String id);

    List<Article> search(Article condition);

    /**
     * 批量查询文章列表
     * @return
     */
    List<Article> search_batch(@Param("_ArticleIds") List<String> _ArticleIds);

    Long get_version(@Param("id")String _Id);

}
