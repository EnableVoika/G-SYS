package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.Article;

import java.util.List;

public interface LifeAndLeisureMapper {

    int insert(Article po);

    int update(Article po);

    int delete(String id);

    Article find(String id);

    List<Article> search();

}
