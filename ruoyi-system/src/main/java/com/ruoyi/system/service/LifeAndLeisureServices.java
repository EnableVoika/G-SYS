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
    Article find(String _Id);

}
