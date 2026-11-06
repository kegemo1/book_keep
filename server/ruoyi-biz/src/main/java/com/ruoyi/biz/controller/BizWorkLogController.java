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
import com.ruoyi.biz.domain.BizWorkLog;
import com.ruoyi.biz.domain.dto.TimerStartDTO;
import com.ruoyi.biz.domain.dto.TimerStopDTO;
import com.ruoyi.biz.domain.dto.WorkLogManualDTO;
import com.ruoyi.biz.domain.vo.RunningTimerVO;
import com.ruoyi.biz.domain.vo.TimerStopResultVO;
import com.ruoyi.biz.domain.vo.WorkLogStatVO;
import com.ruoyi.biz.service.IBizWorkLogService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;

/**
 * 工时打卡 控制层
 *
 * @author bookkeepingWhiz
 */
@RestController
@RequestMapping("/biz/worklog")
public class BizWorkLogController extends BaseController
{
    @Autowired
    private IBizWorkLogService workLogService;

    @PreAuthorize("@ss.hasPermi('biz:worklog:list')")
    @GetMapping("/list")
    public TableDataInfo list(BizWorkLog query)
    {
        startPage();
        return getDataTable(workLogService.selectWorkLogList(query));
    }

    @PreAuthorize("@ss.hasPermi('biz:worklog:query')")
    @GetMapping("/{logId}")
    public AjaxResult getInfo(@PathVariable Long logId)
    {
        return success(workLogService.selectWorkLogById(logId));
    }

    /** 当前进行中的计时器（无则返回 null） */
    @PreAuthorize("@ss.hasPermi('biz:worklog:list')")
    @GetMapping("/running")
    public AjaxResult running()
    {
        RunningTimerVO vo = workLogService.selectRunningTimer(getUserId());
        return success(vo);
    }

    /** 开始计时 */
    @PreAuthorize("@ss.hasPermi('biz:worklog:add')")
    @Log(title = "工时打卡", businessType = BusinessType.INSERT)
    @PostMapping("/start")
    public AjaxResult start(@Validated @RequestBody TimerStartDTO dto)
    {
        return success(workLogService.startTimer(dto, getLoginUser()));
    }

    /** 结束计时（跨零点自动按天拆分） */
    @PreAuthorize("@ss.hasPermi('biz:worklog:edit')")
    @Log(title = "工时打卡", businessType = BusinessType.UPDATE)
    @PostMapping("/stop")
    public AjaxResult stop(@Validated @RequestBody TimerStopDTO dto)
    {
        TimerStopResultVO result = workLogService.stopTimer(dto, getLoginUser());
        return success(result);
    }

    /** 手动补录工时 */
    @PreAuthorize("@ss.hasPermi('biz:worklog:add')")
    @Log(title = "工时补录", businessType = BusinessType.INSERT)
    @PostMapping("/manual")
    public AjaxResult manual(@Validated @RequestBody WorkLogManualDTO dto)
    {
        return success(workLogService.addManualLog(dto, getLoginUser()));
    }

    @PreAuthorize("@ss.hasPermi('biz:worklog:edit')")
    @Log(title = "工时打卡", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BizWorkLog log)
    {
        return toAjax(workLogService.updateWorkLog(log));
    }

    /** 作废记录（保留审计痕迹） */
    @PreAuthorize("@ss.hasPermi('biz:worklog:edit')")
    @Log(title = "工时打卡", businessType = BusinessType.UPDATE)
    @PutMapping("/cancel/{logId}")
    public AjaxResult cancel(@PathVariable Long logId, String reason)
    {
        return toAjax(workLogService.cancelWorkLog(logId, reason));
    }

    /** 恢复已作废记录 */
    @PreAuthorize("@ss.hasPermi('biz:worklog:edit')")
    @Log(title = "工时打卡", businessType = BusinessType.UPDATE)
    @PutMapping("/restore/{logId}")
    public AjaxResult restore(@PathVariable Long logId)
    {
        return toAjax(workLogService.restoreWorkLog(logId));
    }

    @PreAuthorize("@ss.hasPermi('biz:worklog:remove')")
    @Log(title = "工时打卡", businessType = BusinessType.DELETE)
    @DeleteMapping("/{logIds}")
    public AjaxResult remove(@PathVariable Long[] logIds)
    {
        return toAjax(workLogService.deleteWorkLogByIds(logIds));
    }

    // ------------------------------------------------------------------
    // 统计
    // ------------------------------------------------------------------

    /** 按日期统计工时（趋势图） */
    @PreAuthorize("@ss.hasPermi('biz:worklog:list')")
    @GetMapping("/stat/date")
    public AjaxResult statByDate(BizWorkLog query)
    {
        List<WorkLogStatVO> list = workLogService.statByDate(query);
        return success(list);
    }

    /** 按项目统计工时（项目工时占比） */
    @PreAuthorize("@ss.hasPermi('biz:worklog:list')")
    @GetMapping("/stat/project")
    public AjaxResult statByProject(BizWorkLog query)
    {
        return success(workLogService.statByProject(query));
    }

    /** 按人统计工时（团队看板） */
    @PreAuthorize("@ss.hasPermi('biz:worklog:list')")
    @GetMapping("/stat/user")
    public AjaxResult statByUser(BizWorkLog query)
    {
        return success(workLogService.statByUser(query));
    }

    /** 合计工时 */
    @PreAuthorize("@ss.hasPermi('biz:worklog:list')")
    @GetMapping("/stat/total")
    public AjaxResult statTotal(BizWorkLog query)
    {
        return success(workLogService.statTotal(query));
    }

    /** 手动触发超时计时器清理 */
    @PreAuthorize("@ss.hasPermi('biz:worklog:edit')")
    @Log(title = "计时器清理", businessType = BusinessType.UPDATE)
    @PutMapping("/cleanOvertime")
    public AjaxResult cleanOvertime()
    {
        return success(workLogService.cleanOvertimeTimer());
    }

    @PreAuthorize("@ss.hasPermi('biz:worklog:export')")
    @Log(title = "工时打卡", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BizWorkLog query)
    {
        List<BizWorkLog> list = workLogService.selectWorkLogList(query);
        ExcelUtil<BizWorkLog> util = new ExcelUtil<>(BizWorkLog.class);
        util.exportExcel(response, list, "工时打卡数据");
    }
}
