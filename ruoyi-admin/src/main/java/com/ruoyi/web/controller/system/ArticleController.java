package com.ruoyi.web.controller.system;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceExcept;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.Article;
import com.ruoyi.system.service.LifeAndLeisureServices;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Controller
@RequestMapping("/system/article")
public class ArticleController extends BaseController {

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
    public TableDataInfo textList(Article condition)
    {
        startPage();
        List<Article> documentList = services.search(condition);
        return getDataTable(documentList);
    }

    @RequiresPermissions("system:article:view")
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable("id") String _Id, ModelMap mmap)
    {
        if (StringUtils.isEmpty(_Id))
        {
            throw new ServiceExcept("文章id不能为空");
        }
        Article data = services.find_article(_Id);
        if (null == data)
        {
            throw new ServiceExcept("文章不存在");
        }
        mmap.put("title",data.getTitle());
        mmap.put("content",data.getContent());
        mmap.put("author",data.getAuthor());
        mmap.put("source",data.getSource());
        mmap.put("archiveNo",data.getArchiveNo());
        mmap.put("tags",data.getTags());
        return prefix + "/detail";
    }

    @RequiresPermissions("system:article:view")
    @GetMapping("/favorite")
    public String favorite()
    {
        return prefix + "/favorite";
    }

    @GetMapping("/favorite/list")
    @ResponseBody
    public TableDataInfo favorite_list()
    {
        SysUser user = getSysUser();
        startPage();
        List<Article> data = services.find_favorite(user.getUserId());
        return getDataTable(data);
    }

    @RequiresPermissions("system:article:add")
    @GetMapping("/add")
    public String add()
    {
        return prefix + "/add";
    }

    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add_article(Article dto)
    {
        dto.setCreateBy(String.valueOf(getUserId()));
        if (1 == services.add_article(dto))
        {
            return AjaxResult.ok("发布成功");
        }
        return AjaxResult.fail("发布失败,请稍后重试");
    }

    @RequiresPermissions("system:article:view")
    @GetMapping("/my_publish")
    public String my_publish()
    {
        return prefix + "/my_publish";
    }

    @GetMapping("/my_publish/list")
    @ResponseBody
    public TableDataInfo my_publish_list()
    {
        startPage();
        Article condition = new Article();
        condition.setCreateBy(String.valueOf(getUserId()));
        List<Article> data = services.search(condition);
        return getDataTable(data);
    }

    @RequiresPermissions("system:article:edit")
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id")String _Id,ModelMap mmap)
    {
        if (StringUtils.isEmpty(_Id))
        {
            throw new ServiceExcept("文章id不能为空");
        }
        Article data = services.find_article(_Id);
        if (null == data)
        {
            throw new ServiceExcept("文章不存在");
        }
        mmap.put("article",data);
        return prefix + "/edit";
    }

    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit_post(Article dto)
    {
        dto.setUpdateBy(String.valueOf(getUserId()));
        if (1 == services.edit_article(dto))
            return AjaxResult.ok("修改成功");
        return AjaxResult.ok("修改失败");
    }

}
