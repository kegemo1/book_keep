package com.ruoyi.biz.controller.wx;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.biz.constant.BizConstants;
import com.ruoyi.biz.domain.BizWorkLog;
import com.ruoyi.biz.domain.dto.TimerStartDTO;
import com.ruoyi.biz.domain.dto.TimerStopDTO;
import com.ruoyi.biz.domain.dto.WorkLogManualDTO;
import com.ruoyi.biz.domain.vo.TimerStopResultVO;
import com.ruoyi.biz.domain.vo.WorkLogStatVO;
import com.ruoyi.biz.service.IBizWorkLogService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import jakarta.validation.Valid;

/**
 * 小程序 · 工时打卡
 *
 * <p>安全约定：所有查询强制以当前登录用户为过滤条件，小程序端无法查看他人数据。
 *
 * @author bookkeepingWhiz
 */
@RestController
@RequestMapping("/wx/worklog")
public class WxWorkLogController extends BaseController
{
    @Autowired
    private IBizWorkLogService workLogService;

    /** 当前进行中的计时器 */
    @GetMapping("/running")
    public AjaxResult running()
    {
        return AjaxResult.success(workLogService.selectRunningTimer(getUserId()));
    }

    /** 开始计时 */
    @PostMapping("/start")
    public AjaxResult start(@Valid @RequestBody TimerStartDTO dto)
    {
        dto.setSource(BizConstants.Source.MINIAPP);
        return AjaxResult.success("计时已开始", workLogService.startTimer(dto, getLoginUser()));
    }

    /** 结束计时 */
    @PostMapping("/stop")
    public AjaxResult stop(@Valid @RequestBody TimerStopDTO dto)
    {
        TimerStopResultVO result = workLogService.stopTimer(dto, getLoginUser());
        String msg = result.getDiscarded() != null && result.getDiscarded() ? "已丢弃该次计时" : "计时已结束";
        return AjaxResult.success(msg, result);
    }

    /** 手动补录 */
    @PostMapping("/manual")
    public AjaxResult manual(@Valid @RequestBody WorkLogManualDTO dto)
    {
        dto.setSource(BizConstants.Source.MINIAPP);
        return AjaxResult.success("补录成功", workLogService.addManualLog(dto, getLoginUser()));
    }

    /** 我的打卡记录 */
    @GetMapping("/list")
    public TableDataInfo list(BizWorkLog query)
    {
        query.setUserId(getUserId());
        startPage();
        return getDataTable(workLogService.selectWorkLogList(query));
    }

    /** 今日清单 + 今日合计 */
    @GetMapping("/today")
    public AjaxResult today()
    {
        // 注意：selectWorkLogList / selectTotalStat 只认 params 里的日期区间，
        // 实体上的 logDate 不参与查询，只设置属性会导致查出全部日期的数据
        String today = com.ruoyi.common.utils.DateUtils
                .parseDateToStr(com.ruoyi.common.utils.DateUtils.YYYY_MM_DD,
                        com.ruoyi.common.utils.DateUtils.getNowDate());

        BizWorkLog listQuery = new BizWorkLog();
        listQuery.setUserId(getUserId());
        listQuery.getParams().put("beginLogDate", today);
        listQuery.getParams().put("endLogDate", today);

        BizWorkLog statQuery = new BizWorkLog();
        statQuery.setUserId(getUserId());
        statQuery.getParams().put("beginLogDate", today);
        statQuery.getParams().put("endLogDate", today);

        WorkLogStatVO stat = workLogService.statTotal(statQuery);
        AjaxResult ajax = AjaxResult.success();
        ajax.put("totalHours", stat.getTotalHours());
        ajax.put("billableHours", stat.getBillableHours());
        ajax.put("recordCount", stat.getRecordCount());
        ajax.put("running", workLogService.selectRunningTimer(getUserId()));
        ajax.put("list", workLogService.selectWorkLogList(listQuery));
        return ajax;
    }

    /** 我的工时趋势（近 N 天，默认 7 天） */
    @GetMapping("/stat/daily")
    public AjaxResult statDaily(BizWorkLog query)
    {
        query.setUserId(getUserId());
        List<WorkLogStatVO> list = workLogService.statByDate(query);
        return AjaxResult.success(list);
    }

    /** 作废一条记录 */
    @PostMapping("/cancel/{logId}")
    public AjaxResult cancel(@PathVariable Long logId)
    {
        return toAjax(workLogService.cancelWorkLog(logId, "小程序端作废"));
    }
}
