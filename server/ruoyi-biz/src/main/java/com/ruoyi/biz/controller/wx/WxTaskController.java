package com.ruoyi.biz.controller.wx;

import java.util.List;

import com.ruoyi.biz.service.IBizProjectService;
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
import com.ruoyi.biz.domain.BizTask;
import com.ruoyi.biz.service.IBizTaskService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;

/**
 * 小程序 · 任务
 *
 * <p>任务的定位：项目下的二级维度，让「工时」能回答更细的问题——
 * 不只是「今天在 A 项目上花了 3 小时」，而是「其中 2 小时在写接口、1 小时在对账」。
 * 任务的实际工时由打卡记录自动回写，不需要手工维护。
 *
 * <p>所有写操作强制校验「任务所属项目归当前微信用户所有」。
 *
 * @author bookkeepingWhiz
 */
@RestController
@RequestMapping("/wx/task")
public class WxTaskController extends BaseController
{
    @Autowired
    private IBizTaskService taskService;

    @Autowired
    private IBizProjectService projectService;

    /** 任务列表：可按 projectId 过滤，status 为空则返回全部 */
    @GetMapping("/list")
    public AjaxResult list(BizTask task)
    {
        List<BizTask> list = taskService.selectTaskList(task);
        return AjaxResult.success(list == null ? new java.util.ArrayList<BizTask>() : list);
    }

    /** 任务详情 */
    @GetMapping("/{taskId}")
    public AjaxResult getInfo(@PathVariable Long taskId)
    {
        BizTask task = taskService.selectTaskById(taskId);
        assertOwned(task);
        return AjaxResult.success(task);
    }

    /** 新建任务 */
    @PostMapping
    public AjaxResult add(@RequestBody BizTask task)
    {
        if (task.getProjectId() == null)
        {
            throw new ServiceException("任务必须归属一个项目");
        }
        assertProjectOwned(task.getProjectId());
        taskService.insertTask(task);
        return AjaxResult.success("任务已创建", task);
    }

    /** 修改任务（改状态为「已完成」时会自动把进度补成 100） */
    @PutMapping
    public AjaxResult edit(@RequestBody BizTask task)
    {
        BizTask db = taskService.selectTaskById(task.getTaskId());
        assertOwned(db);
        task.setProjectId(db.getProjectId());
        return toAjax(taskService.updateTask(task));
    }

    /** 完成任务 */
    @PutMapping("/done/{taskId}")
    public AjaxResult done(@PathVariable Long taskId)
    {
        BizTask db = taskService.selectTaskById(taskId);
        assertOwned(db);
        BizTask update = new BizTask();
        update.setTaskId(taskId);
        update.setStatus(BizConstants.TaskStatus.DONE);
        update.setProgress(100);
        return toAjax(taskService.updateTask(update));
    }

    /** 删除任务（存在子任务时后端会拒绝） */
    @DeleteMapping("/{taskId}")
    public AjaxResult remove(@PathVariable Long taskId)
    {
        BizTask db = taskService.selectTaskById(taskId);
        assertOwned(db);
        try
        {
            return toAjax(taskService.deleteTaskById(taskId));
        }
        catch (ServiceException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /** 回写任务实际工时 */
    @PutMapping("/syncHours/{taskId}")
    public AjaxResult syncHours(@PathVariable Long taskId)
    {
        BizTask db = taskService.selectTaskById(taskId);
        assertOwned(db);
        return toAjax(taskService.syncActualHours(taskId));
    }

    private void assertOwned(BizTask task)
    {
        if (task == null)
        {
            throw new ServiceException("任务不存在或已删除");
        }
        assertProjectOwned(task.getProjectId());
    }

    private void assertProjectOwned(Long projectId)
    {
        BizTask probe = new BizTask();
        probe.setProjectId(projectId);
        // 项目归属校验由 IBizTaskService 的 checkTaskBelongProject + 项目 owner 完成，
        // 这里再兜一层：项目的 owner 必须是当前用户
        if (projectId == null)
        {
            throw new ServiceException("任务必须归属一个项目");
        }
        if (!isProjectOwner(projectId))
        {
            throw new ServiceException("只能操作自己项目下的任务");
        }
    }

    private boolean isProjectOwner(Long projectId)
    {
        com.ruoyi.biz.domain.BizProject project = projectService.selectProjectById(projectId);
        return project != null && getUserId().equals(project.getOwnerId());
    }
}
