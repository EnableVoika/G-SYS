package com.ruoyi.system.service.impl;

import com.ruoyi.system.domain.Article;
import com.ruoyi.system.mapper.LifeAndLeisureMapper;
import com.ruoyi.system.service.LifeAndLeisureServices;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class LifeAndLeisureServicesImpl implements LifeAndLeisureServices {

    @Resource
    private LifeAndLeisureMapper dao;

    /**
     * 查询文章
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
}
