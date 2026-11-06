package com.ruoyi.biz.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
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
import com.ruoyi.biz.domain.BizProject;
import com.ruoyi.biz.domain.BizProjectMember;
import com.ruoyi.biz.service.IBizProjectService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;

/**
 * 项目 控制层
 *
 * @author bookkeepingWhiz
 */
@RestController
@RequestMapping("/biz/project")
public class BizProjectController extends BaseController
{
    @Autowired
    private IBizProjectService projectService;

    /** 项目列表（带部门数据权限） */
    @PreAuthorize("@ss.hasPermi('biz:project:list')")
    @GetMapping("/list")
    public TableDataInfo list(BizProject project)
    {
        startPage();
        return getDataTable(projectService.selectProjectList(project));
    }

    /** 项目下拉选项（打卡/记账时选择项目用） */
    @PreAuthorize("@ss.hasPermi('biz:project:list')")
    @GetMapping("/options")
    public AjaxResult options(BizProject project)
    {
        return success(projectService.selectProjectOptions(project));
    }

    /** 项目详情 */
    @PreAuthorize("@ss.hasPermi('biz:project:query')")
    @GetMapping("/{projectId}")
    public AjaxResult getInfo(@PathVariable Long projectId)
    {
        return success(projectService.selectProjectById(projectId));
    }

    /** 校验项目编号唯一性 */
    @PreAuthorize("@ss.hasPermi('biz:project:list')")
    @GetMapping("/checkCodeUnique")
    public AjaxResult checkCodeUnique(BizProject project)
    {
        return success(projectService.checkProjectCodeUnique(project));
    }

    @PreAuthorize("@ss.hasPermi('biz:project:add')")
    @Log(title = "项目管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody BizProject project)
    {
        return toAjax(projectService.insertProject(project));
    }

    @PreAuthorize("@ss.hasPermi('biz:project:edit')")
    @Log(title = "项目管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody BizProject project)
    {
        return toAjax(projectService.updateProject(project));
    }

    /** 结项归档（保留历史工时与流水） */
    @PreAuthorize("@ss.hasPermi('biz:project:edit')")
    @Log(title = "项目管理", businessType = BusinessType.UPDATE)
    @PutMapping("/archive/{projectId}")
    public AjaxResult archive(@PathVariable Long projectId)
    {
        return toAjax(projectService.archiveProject(projectId));
    }

    @PreAuthorize("@ss.hasPermi('biz:project:remove')")
    @Log(title = "项目管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{projectIds}")
    public AjaxResult remove(@PathVariable Long[] projectIds)
    {
        return toAjax(projectService.deleteProjectByIds(projectIds));
    }

    @PreAuthorize("@ss.hasPermi('biz:project:export')")
    @Log(title = "项目管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BizProject project)
    {
        List<BizProject> list = projectService.selectProjectList(project);
        ExcelUtil<BizProject> util = new ExcelUtil<>(BizProject.class);
        util.exportExcel(response, list, "项目数据");
    }

    // ------------------------------------------------------------------
    // 项目成员
    // ------------------------------------------------------------------

    @PreAuthorize("@ss.hasPermi('biz:project:list')")
    @GetMapping("/member/list")
    public TableDataInfo memberList(BizProjectMember member)
    {
        startPage();
        return getDataTable(projectService.selectMemberList(member));
    }

    @PreAuthorize("@ss.hasPermi('biz:project:add')")
    @Log(title = "项目成员", businessType = BusinessType.INSERT)
    @PostMapping("/member")
    public AjaxResult addMember(@RequestBody BizProjectMember member)
    {
        return toAjax(projectService.insertMember(member));
    }

    @PreAuthorize("@ss.hasPermi('biz:project:edit')")
    @Log(title = "项目成员", businessType = BusinessType.UPDATE)
    @PutMapping("/member")
    public AjaxResult editMember(@RequestBody BizProjectMember member)
    {
        return toAjax(projectService.updateMember(member));
    }

    @PreAuthorize("@ss.hasPermi('biz:project:remove')")
    @Log(title = "项目成员", businessType = BusinessType.DELETE)
    @DeleteMapping("/member/{memberIds}")
    public AjaxResult removeMember(@PathVariable Long[] memberIds)
    {
        return toAjax(projectService.deleteMemberByIds(memberIds));
    }
}
