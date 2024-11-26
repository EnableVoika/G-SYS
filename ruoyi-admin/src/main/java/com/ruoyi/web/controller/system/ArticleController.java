package com.ruoyi.web.controller.system;

import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.core.page.PageDomain;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.page.TableSupport;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.Article;
import com.ruoyi.system.service.LifeAndLeisureServices;
import com.ruoyi.web.controller.domain.vo.DocumentModelVO;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/system/article")
public class ArticleController {

    @Resource
    private LifeAndLeisureServices services;

    private String prefix = "system/article";

    /**
     * 获取文章资讯
     */
    @RequiresPermissions("system:article:view")
    @GetMapping()
    public String article()
    {
        return prefix + "/article";
    }

    /**
     * 查询全文索引数据
     */
    @PostMapping("/article/list")
    @ResponseBody
    public TableDataInfo textList(BaseEntity baseEntity)
    {
        TableDataInfo rspData = new TableDataInfo();
//        List<DocumentModelVO> documentList = new ArrayList<DocumentModelVO>(Arrays.asList(new DocumentModelVO[documents.size()]));
//        Collections.copy(documentList, documents);
        // 查询条件过滤
//        if (StringUtils.isNotEmpty(baseEntity.getSearchValue()))
//        {
//            documentList.clear();
//            for (DocumentModelVO document : documents)
//            {
//                boolean indexFlag = false;
//                if (document.getTitle().contains(baseEntity.getSearchValue()))
//                {
//                    indexFlag = true;
//                    document.setTitle(document.getTitle().replace(baseEntity.getSearchValue(), "<font color=\"red\">" + baseEntity.getSearchValue() + "</font>"));
//                }
//                if (document.getContent().contains(baseEntity.getSearchValue()))
//                {
//                    indexFlag = true;
//                    document.setContent(document.getContent().replace(baseEntity.getSearchValue(), "<font color=\"red\">" + baseEntity.getSearchValue() + "</font>"));
//                }
//                if (indexFlag)
//                {
//                    documentList.add(document);
//                }
//            }
//        }
        List<Article> documentList = services.search();
        PageDomain pageDomain = TableSupport.buildPageRequest();
        if (null == pageDomain.getPageNum() || null == pageDomain.getPageSize())
        {
            rspData.setRows(documentList);
            rspData.setTotal(documentList.size());
            return rspData;
        }
        Integer pageNum = (pageDomain.getPageNum() - 1) * 10;
        Integer pageSize = pageDomain.getPageNum() * 10;
        if (pageSize > documentList.size())
        {
            pageSize = documentList.size();
        }
        rspData.setRows(documentList.subList(pageNum, pageSize));
        rspData.setTotal(documentList.size());
        return rspData;
    }

}
