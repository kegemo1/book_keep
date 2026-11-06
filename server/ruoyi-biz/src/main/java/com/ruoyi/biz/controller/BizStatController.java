package com.ruoyi.biz.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.biz.domain.BizProject;
import com.ruoyi.biz.domain.vo.ProjectProfitVO;
import com.ruoyi.biz.service.IBizStatService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 统计报表 控制层
 *
 * @author bookkeepingWhiz
 */
@RestController
@RequestMapping("/biz/stat")
public class BizStatController extends BaseController
{
    @Autowired
    private IBizStatService statService;

    /** 工作台概览 */
    @PreAuthorize("@ss.hasPermi('biz:report:list')")
    @GetMapping("/dashboard")
    public AjaxResult dashboard()
    {
        return success(statService.dashboard(getUserId(), getDeptId()));
    }

    /** 项目盈亏清单 */
    @PreAuthorize("@ss.hasPermi('biz:report:list')")
    @GetMapping("/profit/list")
    public TableDataInfo profitList(BizProject project)
    {
        startPage();
        List<ProjectProfitVO> list = statService.selectProjectProfit(project);
        return getDataTable(list);
    }

    /** 单项目盈亏明细 */
    @PreAuthorize("@ss.hasPermi('biz:report:list')")
    @GetMapping("/profit/{projectId}")
    public AjaxResult profitDetail(@PathVariable Long projectId)
    {
        return success(statService.selectProjectProfitDetail(projectId));
    }
}
