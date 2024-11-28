package com.ruoyi.system.service.impl;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.utils.CacheUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.system.domain.Article;
import com.ruoyi.system.domain.FavoriteArticle;
import com.ruoyi.system.mapper.FavoriteArticleMapper;
import com.ruoyi.system.mapper.LifeAndLeisureMapper;
import com.ruoyi.system.service.LifeAndLeisureServices;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class LifeAndLeisureServicesImpl implements LifeAndLeisureServices {

    @Resource
    private LifeAndLeisureMapper dao;

    @Resource
    private FavoriteArticleMapper fam;

    private void add_cache(Article _Article)
    {
        CacheUtils.put(Constants.ARTICLE_CACHE_NAME,_Article.getTableId(),_Article);
    }

    /**
     * 查询文章列表
     * @return
     */
    @Override
    public List<Article> search() {
        List<Article> pos = dao.search();
        if (CollectionUtils.isEmpty(pos))
        {
            return null;
        }
        return pos;
    }

    /**
     * 查询文章详情
     */
    @Override
    public Article find_article(String _Id)
    {
        Article po = CacheUtils.get(Constants.ARTICLE_CACHE_NAME,_Id);
        if (null != po)
            return po;
        po = dao.find_article(_Id);
        add_cache(po);
        return po;
    }

    /**
     * 查询我的收藏文章
     * @param _UserId
     * @return
     */
    @Override
    public List<Article> find_favorite(long _UserId) {
        List<FavoriteArticle> fas = fam.find(_UserId);
        List<Article> data = null;
        if (CollectionUtils.isNotEmpty(fas))
        {
            List<String> articleIds = new ArrayList<>();
            for (FavoriteArticle datum : fas)
            {
                articleIds.add(datum.getArticleId());
            }
            data = dao.search_batch(articleIds);
        }
        return data;
    }

    @Override
    public int add_article(Article dto) {
        dto.setTableId(IdUtils.fastSimpleUUID());
        int res = dao.add_article(dto);
        if (1 == res)
            add_cache(dto);
        return res;
    }


}
