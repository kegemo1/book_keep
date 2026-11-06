package com.ruoyi.biz.domain.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 工作台 / 小程序首页概览视图
 *
 * @author bookkeepingWhiz
 */
public class DashboardVO
{
    /** 今日工时（小时） */
    private BigDecimal todayHours;

    /** 本周工时（小时） */
    private BigDecimal weekHours;

    /** 本月工时（小时） */
    private BigDecimal monthHours;

    /** 今日计费工时（小时） */
    private BigDecimal todayBillableHours;

    /** 本月收入 */
    private BigDecimal monthIncome;

    /** 本月支出 */
    private BigDecimal monthExpense;

    /** 本月净额（收入 - 支出） */
    private BigDecimal monthProfit;

    /** 待收款（应收未收合计） */
    private BigDecimal receivable;

    /** 进行中的计时器（无则为 null） */
    private RunningTimerVO runningTimer;

    /** 进行中的项目数 */
    private Long runningProjectCount;

    /** 我的待办任务数 */
    private Long todoTaskCount;

    /** 累计工时（小时，不含时间范围过滤，供「我的」页展示个人档案） */
    private BigDecimal totalHours;

    /** 累计打卡条数 */
    private Long totalLogCount;

    /** 近 7 天工时趋势（按日期倒序） */
    private List<WorkLogStatVO> recentDailyHours;

    public BigDecimal getTodayHours() { return todayHours; }
    public void setTodayHours(BigDecimal todayHours) { this.todayHours = todayHours; }

    public BigDecimal getWeekHours() { return weekHours; }
    public void setWeekHours(BigDecimal weekHours) { this.weekHours = weekHours; }

    public BigDecimal getMonthHours() { return monthHours; }
    public void setMonthHours(BigDecimal monthHours) { this.monthHours = monthHours; }

    public BigDecimal getTodayBillableHours() { return todayBillableHours; }
    public void setTodayBillableHours(BigDecimal todayBillableHours) { this.todayBillableHours = todayBillableHours; }

    public BigDecimal getMonthIncome() { return monthIncome; }
    public void setMonthIncome(BigDecimal monthIncome) { this.monthIncome = monthIncome; }

    public BigDecimal getMonthExpense() { return monthExpense; }
    public void setMonthExpense(BigDecimal monthExpense) { this.monthExpense = monthExpense; }

    public BigDecimal getMonthProfit() { return monthProfit; }
    public void setMonthProfit(BigDecimal monthProfit) { this.monthProfit = monthProfit; }

    public BigDecimal getReceivable() { return receivable; }
    public void setReceivable(BigDecimal receivable) { this.receivable = receivable; }

    public RunningTimerVO getRunningTimer() { return runningTimer; }
    public void setRunningTimer(RunningTimerVO runningTimer) { this.runningTimer = runningTimer; }

    public Long getRunningProjectCount() { return runningProjectCount; }
    public void setRunningProjectCount(Long runningProjectCount) { this.runningProjectCount = runningProjectCount; }

    public Long getTodoTaskCount() { return todoTaskCount; }
    public void setTodoTaskCount(Long todoTaskCount) { this.todoTaskCount = todoTaskCount; }

    public BigDecimal getTotalHours() { return totalHours; }
    public void setTotalHours(BigDecimal totalHours) { this.totalHours = totalHours; }

    public Long getTotalLogCount() { return totalLogCount; }
    public void setTotalLogCount(Long totalLogCount) { this.totalLogCount = totalLogCount; }

    public List<WorkLogStatVO> getRecentDailyHours() { return recentDailyHours; }
    public void setRecentDailyHours(List<WorkLogStatVO> recentDailyHours) { this.recentDailyHours = recentDailyHours; }
}
