package com.ruoyi.biz.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.biz.domain.BizFinanceCategory;
import com.ruoyi.biz.service.IBizFinanceService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;

/**
 * 收支类别 控制层
 *
 * @author bookkeepingWhiz
 */
@RestController
@RequestMapping("/biz/category")
public class BizFinanceCategoryController extends BaseController
{
    @Autowired
    private IBizFinanceService financeService;

    @PreAuthorize("@ss.hasPermi('biz:category:list')")
    @GetMapping("/list")
    public TableDataInfo list(BizFinanceCategory category)
    {
        startPage();
        return getDataTable(financeService.selectCategoryList(category));
    }

    /** 完整类别树（父子结构） */
    @PreAuthorize("@ss.hasPermi('biz:category:list')")
    @GetMapping("/tree")
    public AjaxResult tree(BizFinanceCategory category)
    {
        return success(financeService.buildCategoryTree(financeService.selectCategoryList(category)));
    }

    /** 类别下拉选项（记账时选择类别用） */
    @PreAuthorize("@ss.hasPermi('biz:category:list')")
    @GetMapping("/options")
    public AjaxResult options(String categoryType)
    {
        return success(financeService.selectCategoryOptions(categoryType));
    }

    @PreAuthorize("@ss.hasPermi('biz:category:query')")
    @GetMapping("/{categoryId}")
    public AjaxResult getInfo(@PathVariable Long categoryId)
    {
        return success(financeService.selectCategoryById(categoryId));
    }

    @PreAuthorize("@ss.hasPermi('biz:category:list')")
    @GetMapping("/checkNameUnique")
    public AjaxResult checkNameUnique(BizFinanceCategory category)
    {
        return success(financeService.checkCategoryNameUnique(category));
    }

    @PreAuthorize("@ss.hasPermi('biz:category:add')")
    @Log(title = "收支类别", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody BizFinanceCategory category)
    {
        return toAjax(financeService.insertCategory(category));
    }

    @PreAuthorize("@ss.hasPermi('biz:category:edit')")
    @Log(title = "收支类别", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody BizFinanceCategory category)
    {
        return toAjax(financeService.updateCategory(category));
    }

    @PreAuthorize("@ss.hasPermi('biz:category:remove')")
    @Log(title = "收支类别", businessType = BusinessType.DELETE)
    @DeleteMapping("/{categoryId}")
    public AjaxResult remove(@PathVariable Long categoryId)
    {
        return toAjax(financeService.deleteCategoryById(categoryId));
    }
}
