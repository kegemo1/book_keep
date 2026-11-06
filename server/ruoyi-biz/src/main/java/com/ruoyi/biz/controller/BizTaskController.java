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
import com.ruoyi.biz.domain.BizTask;
import com.ruoyi.biz.service.IBizTaskService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;

/**
 * 任务 控制层
 *
 * @author bookkeepingWhiz
 */
@RestController
@RequestMapping("/biz/task")
public class BizTaskController extends BaseController
{
    @Autowired
    private IBizTaskService taskService;

    @PreAuthorize("@ss.hasPermi('biz:task:list')")
    @GetMapping("/list")
    public TableDataInfo list(BizTask task)
    {
        startPage();
        return getDataTable(taskService.selectTaskList(task));
    }

    /** 项目下的任务树 */
    @PreAuthorize("@ss.hasPermi('biz:task:list')")
    @GetMapping("/tree/{projectId}")
    public AjaxResult tree(@PathVariable Long projectId)
    {
        return success(taskService.selectTaskByProjectId(projectId));
    }

    /** 任务下拉选项（打卡时选择任务用） */
    @PreAuthorize("@ss.hasPermi('biz:task:list')")
    @GetMapping("/options")
    public AjaxResult options(BizTask task)
    {
        return success(taskService.selectTaskOptions(task));
    }

    @PreAuthorize("@ss.hasPermi('biz:task:query')")
    @GetMapping("/{taskId}")
    public AjaxResult getInfo(@PathVariable Long taskId)
    {
        return success(taskService.selectTaskById(taskId));
    }

    @PreAuthorize("@ss.hasPermi('biz:task:list')")
    @GetMapping("/checkNameUnique")
    public AjaxResult checkNameUnique(BizTask task)
    {
        return success(taskService.checkTaskNameUnique(task));
    }

    @PreAuthorize("@ss.hasPermi('biz:task:add')")
    @Log(title = "任务管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody BizTask task)
    {
        return toAjax(taskService.insertTask(task));
    }

    @PreAuthorize("@ss.hasPermi('biz:task:edit')")
    @Log(title = "任务管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody BizTask task)
    {
        return toAjax(taskService.updateTask(task));
    }

    /** 手动触发单个任务的实际工时回写 */
    @PreAuthorize("@ss.hasPermi('biz:task:edit')")
    @Log(title = "任务工时回写", businessType = BusinessType.UPDATE)
    @PutMapping("/syncHours/{taskId}")
    public AjaxResult syncHours(@PathVariable Long taskId)
    {
        return toAjax(taskService.syncActualHours(taskId));
    }

    /** 手动触发全量工时回写 */
    @PreAuthorize("@ss.hasPermi('biz:task:edit')")
    @Log(title = "任务工时回写", businessType = BusinessType.UPDATE)
    @PutMapping("/syncHours/all")
    public AjaxResult syncAllHours()
    {
        return success(taskService.syncAllTaskActualHours());
    }

    @PreAuthorize("@ss.hasPermi('biz:task:remove')")
    @Log(title = "任务管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{taskIds}")
    public AjaxResult remove(@PathVariable Long[] taskIds)
    {
        return toAjax(taskService.deleteTaskByIds(taskIds));
    }

    @PreAuthorize("@ss.hasPermi('biz:task:list')")
    @Log(title = "任务管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BizTask task)
    {
        List<BizTask> list = taskService.selectTaskList(task);
        ExcelUtil<BizTask> util = new ExcelUtil<>(BizTask.class);
        util.exportExcel(response, list, "任务数据");
    }
}
