package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.FavoriteArticle;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FavoriteArticleMapper {

    List<FavoriteArticle> find(@Param("userId") long _UserId);

    int insert_batch(@Param("list") List<FavoriteArticle> list);

}
