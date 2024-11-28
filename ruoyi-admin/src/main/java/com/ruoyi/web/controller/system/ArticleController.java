package com.ruoyi.web.controller.system;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.core.page.PageDomain;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.page.TableSupport;
import com.ruoyi.common.exception.ServiceExcept;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.Article;
import com.ruoyi.system.service.LifeAndLeisureServices;
import com.ruoyi.web.controller.domain.vo.DocumentModelVO;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

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
    @PostMapping("/list")
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

    @GetMapping("/detail/{id}")
    public String detail(@PathVariable("id") String _Id, ModelMap mmap)
    {
        if (StringUtils.isEmpty(_Id))
        {
            throw new ServiceExcept("文章id不能为空");
        }
        Article data = services.find(_Id);
        if (null == data)
        {
            throw new ServiceExcept("文章不存在");
        }
//        mmap.put("content","    龙帝的<b>后穴向外暴露</b>着,鸡巴一下一下在众人面前跳动，玲口有溢出的乳白色精液。龙帝的鸡巴渴望着发泄、渴望着射精，但可惜的是没有儿子的允许，龙帝一滴精液也射不出来。作为一个父亲，却被自己的儿子管控自己射精，龙帝心里十分羞耻，可在欲望面前，龙帝鸡巴已经一个月的悬停在射精的边缘，两颗龙蛋饱满得快要炸了一般难受，终于，龙帝还是红着脸看着儿子、看着龙椅下来自各个国家的代表兽们，极度羞耻说了出来。\n    \"求...求求儿子，让我射出来\"。龙帝淫荡地哀求着。");
//        mmap.put("title","龙帝的沦陷");
        mmap.put("title",data.getTitle());
        mmap.put("content",data.getContent());
        mmap.put("author",data.getAuthor());
        mmap.put("source",data.getSource());
        mmap.put("archiveNo",data.getArchiveNo());
        mmap.put("tags",data.getTags());
        return prefix + "/detail";
    }

}
