package com.ruoyi.system.service.impl;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.exception.ServiceExcept;
import com.ruoyi.common.utils.CacheUtils;
import com.ruoyi.common.utils.ShiroUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.system.domain.Article;
import com.ruoyi.system.domain.ArticleTag;
import com.ruoyi.system.domain.FavoriteArticle;
import com.ruoyi.system.mapper.ArticleTagMapper;
import com.ruoyi.system.mapper.FavoriteArticleMapper;
import com.ruoyi.system.mapper.LifeAndLeisureMapper;
import com.ruoyi.system.service.LifeAndLeisureServices;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class LifeAndLeisureServicesImpl implements LifeAndLeisureServices {

    @Resource
    private LifeAndLeisureMapper dao;

    @Resource
    private FavoriteArticleMapper fam;

    @Resource
    private ArticleTagMapper atm;

    private static void add_cache(Article _Article)
    {
        CacheUtils.put(Constants.ARTICLE_CACHE_NAME,_Article.getTableId(),_Article);
    }

    private static Article get_cache(String _Id)
    {
        return CacheUtils.get(Constants.ARTICLE_CACHE_NAME,_Id);
    }

    private static String separator_symbol[] = {"；",",","，","、","/","。","\\."};

    private static String parse_separator(String tag)
    {
        for (String s : separator_symbol)
        {
            tag = tag.replaceAll(s,";");
        }
        tag = tag.replaceAll(";+",";");
        if (tag.endsWith(";"))
            return tag.substring(0,tag.length() - 1);
        return tag;
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
    @Transactional
    public int add_article(Article dto) {
        dto.setTableId(IdUtils.fastSimpleUUID());
        String tag = dto.getTags();
        if (StringUtils.isNotEmpty(tag))
        {
            tag = parse_separator(tag);
            dto.setTags(tag);
            List<String> tags = Arrays.asList(tag.split(";"));
            ArrayList<ArticleTag> articleTags = new ArrayList<>();
            for (String x : tags)
            {
                ArticleTag articleTag = new ArticleTag(dto.getTableId(),x);
                articleTags.add(articleTag);
            }
            atm.insert_batch(articleTags);
        }
        int res = dao.add_article(dto);
        add_cache(find_article(dto.getTableId()));
        return res;
    }

    @Override
    @Transactional
    public int edit_article(Article dto) {
        Article po =  dao.find_article(dto.getTableId());
        if (null == po)
            throw new ServiceExcept("文章不存在");
        if ( !"1".equals(dto.getUpdateBy()) && !dto.getUpdateBy().equals(po.getCreateBy()))
            throw new ServiceExcept("你没有权限修改这篇文章");
        // 虽然编译器能处理自加传参的先后顺序，但我还是不喜欢这么写
        dto.setVersion(po.getVersion() + 1);
        String tag = dto.getTags();
        atm.remove(dto.getTableId());
        if (StringUtils.isNotEmpty(tag))
        {
            tag = parse_separator(tag);
            dto.setTags(tag);
            List<String> tags = Arrays.asList(tag.split(";"));
            ArrayList<ArticleTag> articleTags = new ArrayList<>();
            for (String x : tags)
            {
                ArticleTag articleTag = new ArticleTag(dto.getTableId(),x);
                articleTags.add(articleTag);
            }
            atm.insert_batch(articleTags);
        }
        int res = dao.edit_article(dto);
        add_cache(dao.find_article(dto.getTableId()));
        return res;
    }

    @Override
    public int del_article(String _Id) {
        Article po =  dao.find_article(_Id);
        if (null == po)
            throw new ServiceExcept("文章不存在");
        if ( 1L != ShiroUtils.getUserId() && !Long.valueOf(po.getCreateBy()).equals(ShiroUtils.getUserId()))
            throw new ServiceExcept("你没有权限修改这篇文章");
        return dao.del_article(_Id);
    }

    @Override
    public int del_article_batch(List<String> _Ids) {

        return dao.del_article_batch(_Ids);
    }


}
