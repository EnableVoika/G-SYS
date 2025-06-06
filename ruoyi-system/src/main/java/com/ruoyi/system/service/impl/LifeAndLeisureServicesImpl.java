package com.ruoyi.system.service.impl;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceExcept;
import com.ruoyi.common.utils.CacheUtils;
import com.ruoyi.common.utils.PageUtils;
import com.ruoyi.common.utils.ShiroUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.system.domain.Article;
import com.ruoyi.system.domain.ArticleHistory;
import com.ruoyi.system.domain.ArticleTag;
import com.ruoyi.system.domain.FavoriteArticle;
import com.ruoyi.system.mapper.*;
import com.ruoyi.system.service.LifeAndLeisureServices;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

@Service
public class LifeAndLeisureServicesImpl implements LifeAndLeisureServices {

    private static final Logger log = LoggerFactory.getLogger(LifeAndLeisureServicesImpl.class);
    @Resource
    private LifeAndLeisureMapper dao;

    @Resource
    private FavoriteArticleMapper fam;

    @Resource
    private ArticleTagMapper atm;

    @Resource
    private ArticleHistoryMapper ahm;

    @Resource
    private SysDictDataMapper sddm;

    private static void add_cache(Article _Article)
    {
        CacheUtils.put(Constants.ARTICLE_CACHE_NAME,_Article.getTableId(),_Article);
    }

    private static Article get_cache(String _Id)
    {
        return CacheUtils.get(Constants.ARTICLE_CACHE_NAME,_Id);
    }

    private static void remove_cache(String _Id)
    {
        CacheUtils.remove(Constants.ARTICLE_CACHE_NAME,_Id);
    }

    private static String separator_symbol[] = {"；",",","，","、","/","。","\\."," "};

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

    private boolean can_access_r18()
    {
        // 超级管理员一律允许
        if (1L == ShiroUtils.getUserId())
            return true;
        List<SysDictData> canAccessR18Dict = sddm.selectDictDataByType(Constants.CAN_ACCESS_R18_ROLE_DICT_NAME);
        if (CollectionUtils.isEmpty(canAccessR18Dict))
            return false;
        Set<String> canAccessR18RoleSet = new HashSet<>();
        for (SysDictData sysDictData : canAccessR18Dict)
        {
            canAccessR18RoleSet.add(sysDictData.getDictValue());
        }
        SysUser sysUser = ShiroUtils.getSysUser();
        if (null == sysUser)
            return false;
        List<SysRole> roles = sysUser.getRoles();
        if (CollectionUtils.isNotEmpty(roles))
        {
            for (SysRole role : roles)
            {
                for (String s : canAccessR18RoleSet)
                {
                    if (role.getRoleKey().equals(s) || role.getRoleName().equals(s))
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * 查询文章列表
     * @return
     */
    @Override
    public List<Article> search(Article condition) {
        if (!can_access_r18())
            condition.setR18(0);
        PageUtils.startPage();
        return dao.search(condition);
    }

    /**
     * 查询我发布文章列表
     * 因为涉及到r18内容过滤，自己的文章不过滤，所以单独一个新的函数
     * @return
     */
    @Override
    public List<Article> search_my_publish(Article condition) {
        PageUtils.startPage();
        return dao.search(condition);
    }

    private static boolean has_access_article(Long _CurrentUserId, Article _Po,int _SpecialAccess)
    {
        if (1 == _Po.getDel())
            return false;
        // 如果授权了特别访问，直接允许访问
        if (1 == _SpecialAccess)
            return true;
        // 先判断当前用户是不是这篇文章的主人
        int isMaster = 0;
        if (_CurrentUserId.equals(Long.valueOf(_Po.getCreateBy())))
            isMaster = 1;
        // 如果不是文章主人。开始验证各项权限是否都通过，通过才能访问
        if (0 == isMaster)
        {
            return 0 == _Po.getStatus();
        }
       return true;
    }

    /**
     * 查询文章详情
     */
    @Override
    public Article find_article(String _Id)
    {
        Article po = get_cache(_Id);
        if (null == po)
        {
            if (null == (po = dao.find_article(_Id)))
                return null;
            add_cache(po);
        }
        Long currentUserId = ShiroUtils.getUserId();
        if (!has_access_article(currentUserId,po,0))
        {
            log.error("当前用户{ID:{},名称:{},账号:{}},访问文章时没有权限，文章对象{}",currentUserId,ShiroUtils.getSysUser().getUserName(),ShiroUtils.getSysUser().getLoginName(),po);
            throw new ServiceExcept("没有权限访问该文章");
        }
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
            PageUtils.startPage();
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
            String[] tags = tag.split(";");
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
        {
            log.error("请求参数对象:{}",dto);
            throw new ServiceExcept("文章不存在");
        }
        if ( !"1".equals(dto.getUpdateBy()) && !dto.getUpdateBy().equals(po.getCreateBy()))
        {
            log.error("dto.updateBy(也是当前用户)={},po.createBy={}",dto.getUpdateBy(),po.getCreateBy());
            throw new ServiceExcept("你没有权限修改这篇文章");
        }
        // 虽然编译器能处理自加传参的先后顺序，但我还是不喜欢这么写
        dto.setVersion(po.getVersion() + 1);
        ArticleHistory articleHistory = new ArticleHistory(po);
        articleHistory.setCreateBy(String.valueOf(ShiroUtils.getUserId()));
        ahm.insert(articleHistory);
        String tag = dto.getTags();
        atm.remove(dto.getTableId());
        if (StringUtils.isNotEmpty(tag))
        {
            tag = parse_separator(tag);
            dto.setTags(tag);
            String[] tags = tag.split(";");
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

    /**
     * 保存文章，不记录历史记录
     * 并且文章状态恒定为草稿 status = 2
     * @param dto
     * @return
     */
    @Transactional
    @Override
    public int save_article(Article dto) {
        Article po =  dao.find_article(dto.getTableId());
        if (null == po)
        {
            log.error("请求参数对象:{}",dto);
            throw new ServiceExcept("文章不存在");
        }
        if ( !"1".equals(dto.getUpdateBy()) && !dto.getUpdateBy().equals(po.getCreateBy()))
        {
            log.error("save_article():dto.updateBy(也是当前用户)={},po.createBy={}",dto.getUpdateBy(),po.getCreateBy());
            throw new ServiceExcept("你没有权限修改这篇文章");
        }
        String tag = dto.getTags();
        atm.remove(dto.getTableId());
        if (StringUtils.isNotEmpty(tag))
        {
            tag = parse_separator(tag);
            dto.setTags(tag);
            String[] tags = tag.split(";");
            ArrayList<ArticleTag> articleTags = new ArrayList<>();
            for (String x : tags)
            {
                ArticleTag articleTag = new ArticleTag(dto.getTableId(),x);
                articleTags.add(articleTag);
            }
            atm.insert_batch(articleTags);
        }
        int res = dao.save_article(dto);
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
        remove_cache(_Id);
        return dao.del_article(_Id);
    }

    @Override
    public int del_article_batch(List<String> _Ids)
    {
        if (CollectionUtils.isEmpty(_Ids))
            return 0;
        for (String id : _Ids)
        {
            remove_cache(id);
        }
        return dao.del_article_batch(_Ids);
    }


}
