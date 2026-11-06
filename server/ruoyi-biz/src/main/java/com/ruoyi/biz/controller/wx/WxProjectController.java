package com.ruoyi.biz.controller.wx;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.biz.constant.BizConstants;
import com.ruoyi.biz.domain.BizProject;
import com.ruoyi.biz.domain.BizTask;
import com.ruoyi.biz.service.IBizProjectService;
import com.ruoyi.biz.service.IBizStatService;
import com.ruoyi.biz.service.IBizTaskService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;

/**
 * 小程序 · 项目与工作台
 *
 * <p>后台账号与微信用户彻底分离后，小程序是微信用户创建/维护项目的唯一入口，
 * 因此这里补齐了项目的增删改查。所有写操作都强制校验「项目归属当前微信用户」。
 *
 * @author bookkeepingWhiz
 */
@RestController
@RequestMapping("/wx/project")
public class WxProjectController extends BaseController
{
    @Autowired
    private IBizProjectService projectService;

    @Autowired
    private IBizTaskService taskService;

    @Autowired
    private IBizStatService statService;

    /** 我的项目列表（status 为空时默认返回进行中的项目） */
    @GetMapping("/list")
    public AjaxResult list(BizProject project)
    {
        // 小程序端强制只返回当前微信用户创建的项目（下拉/列表均不允许出现他人项目）
        project.setOwnerId(getUserId());
        if (StringUtils.isEmpty(project.getStatus()))
        {
            project.setStatus(BizConstants.ProjectStatus.RUNNING);
        }
        return AjaxResult.success(projectService.selectProjectOptions(project));
    }

    /** 项目详情（编辑前回显用，强制归属校验） */
    @GetMapping("/{projectId}")
    public AjaxResult getInfo(@PathVariable Long projectId)
    {
        BizProject project = projectService.selectProjectById(projectId);
        assertOwner(project);
        return AjaxResult.success(project);
    }

    /** 新建项目：负责人自动取当前微信用户 */
    @PostMapping
    public AjaxResult add(@RequestBody BizProject project)
    {
        // 强制归属：避免小程序端伪造 ownerId 把项目挂到别人名下
        project.setOwnerId(getUserId());
        projectService.insertProject(project);
        return AjaxResult.success("项目已创建", project);
    }

    /** 修改项目（强制归属校验） */
    @PutMapping
    public AjaxResult edit(@RequestBody BizProject project)
    {
        BizProject db = projectService.selectProjectById(project.getProjectId());
        assertOwner(db);
        project.setOwnerId(db.getOwnerId());
        return toAjax(projectService.updateProject(project));
    }

    /** 删除项目（强制归属校验；存在任务或流水时后端会拒绝） */
    @DeleteMapping("/{projectId}")
    public AjaxResult remove(@PathVariable Long projectId)
    {
        BizProject db = projectService.selectProjectById(projectId);
        assertOwner(db);
        try
        {
            return toAjax(projectService.deleteProjectById(projectId));
        }
        catch (ServiceException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /** 项目下的任务下拉（只返回未完成的） */
    @GetMapping("/taskOptions/{projectId}")
    public AjaxResult taskOptions(@PathVariable Long projectId)
    {
        BizTask query = new BizTask();
        query.setProjectId(projectId);
        List<BizTask> tasks = taskService.selectTaskOptions(query);
        return AjaxResult.success(tasks == null ? new java.util.ArrayList<BizTask>() : tasks);
    }

    /** 项目盈亏（只看自己的项目） */
    @GetMapping("/profit")
    public AjaxResult profit(BizProject project)
    {
        // 微信用户没有角色，@DataScope 生成不出任何条件，必须显式按 owner_id 隔离，
        // 否则会把全库项目（含他人收入成本）一起返回
        project.setOwnerId(getUserId());
        return AjaxResult.success(statService.selectProjectProfit(project));
    }

    /** 首页概览 */
    @GetMapping("/dashboard")
    public AjaxResult dashboard()
    {
        return AjaxResult.success(statService.dashboard(getUserId(), getDeptId()));
    }

    /**
     * 归属校验：微信用户只能操作自己创建的项目
     */
    private void assertOwner(BizProject project)
    {
        if (project == null)
        {
            throw new ServiceException("项目不存在或已删除");
        }
        if (!getUserId().equals(project.getOwnerId()))
        {
            throw new ServiceException("只能操作自己创建的项目");
        }
    }
}
