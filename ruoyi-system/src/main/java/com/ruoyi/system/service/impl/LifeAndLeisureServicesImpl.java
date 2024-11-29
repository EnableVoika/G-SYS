package com.ruoyi.system.service.impl;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.exception.ServiceExcept;
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

    private static void add_cache(Article _Article)
    {
        CacheUtils.put(Constants.ARTICLE_CACHE_NAME,_Article.getTableId(),_Article);
    }

    private static Article get_cache(String _Id)
    {
        return CacheUtils.get(Constants.ARTICLE_CACHE_NAME,_Id);
    }

    /**
     * 查询文章列表
     * @return
     */
    @Override
    public List<Article> search(Article condition) {
        List<Article> pos = dao.search(condition);
        return pos;
    }

    /**
     * 查询文章详情
     */
    @Override
    public Article find_article(String _Id)
    {
        Article po = get_cache(_Id);
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
        add_cache(find_article(dto.getTableId()));
        return res;
    }

    @Override
    public int edit_article(Article dto) {
        Long currentVersion =  dao.get_version(dto.getTableId());
        if (null == currentVersion)
            throw new ServiceExcept("文章不存在");
        ++currentVersion;
        // 虽然编译器能处理自加传参的先后顺序，但我还是不喜欢这么写
        dto.setVersion(currentVersion);
        int res = dao.edit_article(dto);
        add_cache(dao.find_article(dto.getTableId()));
        return res;
    }


}
