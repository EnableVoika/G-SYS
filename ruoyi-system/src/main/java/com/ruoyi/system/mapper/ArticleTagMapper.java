package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.ArticleTag;
import com.ruoyi.system.domain.FavoriteArticle;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ArticleTagMapper {

    List<ArticleTag> find_by_article_id(@Param("articleId") long _ArticleId);

    int insert_batch(@Param("list") List<ArticleTag> list);

    int remove(@Param("id") String id);

}
