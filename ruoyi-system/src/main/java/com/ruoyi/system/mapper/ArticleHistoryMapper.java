package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.ArticleHistory;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ArticleHistoryMapper {

    int insert(ArticleHistory po);

    ArticleHistory find_by_article_id(String _ArticleId);

    List<ArticleHistory> search(ArticleHistory condition);

    List<ArticleHistory> search_batch(@Param("_ArticleIds") List<String> _ArticleIds);

}
