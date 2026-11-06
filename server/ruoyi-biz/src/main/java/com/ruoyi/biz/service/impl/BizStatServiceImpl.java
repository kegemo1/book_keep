package com.ruoyi.biz.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.biz.constant.BizConstants;
import com.ruoyi.biz.domain.BizFinanceRecord;
import com.ruoyi.biz.domain.BizProject;
import com.ruoyi.biz.domain.BizWorkLog;
import com.ruoyi.biz.domain.vo.CategoryStatVO;
import com.ruoyi.biz.domain.vo.DashboardVO;
import com.ruoyi.biz.domain.vo.ProjectProfitVO;
import com.ruoyi.biz.domain.vo.WorkLogStatVO;
import com.ruoyi.biz.mapper.BizFinanceRecordMapper;
import com.ruoyi.biz.mapper.BizProjectMapper;
import com.ruoyi.biz.mapper.BizTaskMapper;
import com.ruoyi.biz.service.IBizFinanceService;
import com.ruoyi.biz.service.IBizStatService;
import com.ruoyi.biz.service.IBizWorkLogService;
import com.ruoyi.biz.util.BizDateUtils;
import com.ruoyi.biz.util.BizMathUtils;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.utils.DateUtils;

/**
 * 统计报表 业务层实现
 *
 * <p>核算口径（务必与前端展示保持一致）：
 * <ul>
 *   <li>工时只统计「已完成」记录（log_status=1），按 log_date 归属</li>
 *   <li>毛利 = 收入 - 直接成本；人力成本 = 工时 × 时薪；净利 = 毛利 - 人力成本</li>
 *   <li>应收 = 收入 - 已结算</li>
 * </ul>
 *
 * @author bookkeepingWhiz
 */
@Service
public class BizStatServiceImpl implements IBizStatService
{
    @Autowired
    private IBizWorkLogService workLogService;

    @Autowired
    private IBizFinanceService financeService;

    @Autowired
    private BizFinanceRecordMapper recordMapper;

    @Autowired
    private BizProjectMapper projectMapper;

    @Autowired
    private BizTaskMapper taskMapper;

    @Override
    public DashboardVO dashboard(Long userId, Long deptId)
    {
        DashboardVO vo = new DashboardVO();
        String today = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.getNowDate());

        // 今日工时
        // 注意：selectTotalStat 只认 params 里的日期区间，实体上的 logDate 不参与查询
        BizWorkLog todayQuery = new BizWorkLog();
        todayQuery.setUserId(userId);
        todayQuery.getParams().put("beginLogDate", today);
        todayQuery.getParams().put("endLogDate", today);
        WorkLogStatVO todayStat = workLogService.statTotal(todayQuery);
        vo.setTodayHours(BizMathUtils.secondsToHours(todayStat.getTotalSeconds()));
        vo.setTodayBillableHours(BizMathUtils.secondsToHours(todayStat.getBillableSeconds()));

        // 本周工时
        BizWorkLog weekQuery = new BizWorkLog();
        weekQuery.setUserId(userId);
        weekQuery.getParams().put("beginLogDate",
                DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, BizDateUtils.weekStart()));
        weekQuery.getParams().put("endLogDate", today);
        vo.setWeekHours(BizMathUtils.secondsToHours(workLogService.statTotal(weekQuery).getTotalSeconds()));

        // 本月工时
        BizWorkLog monthQuery = new BizWorkLog();
        monthQuery.setUserId(userId);
        monthQuery.getParams().put("beginLogDate",
                DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, BizDateUtils.monthStart()));
        monthQuery.getParams().put("endLogDate", today);
        vo.setMonthHours(BizMathUtils.secondsToHours(workLogService.statTotal(monthQuery).getTotalSeconds()));

        // 本月收支
        String monthBegin = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, BizDateUtils.monthStart());
        BizFinanceRecord incomeQuery = new BizFinanceRecord();
        incomeQuery.setUserId(userId);
        incomeQuery.setRecordType(BizConstants.RecordType.INCOME);
        incomeQuery.getParams().put("beginRecordDate", monthBegin);
        incomeQuery.getParams().put("endRecordDate", today);
        BigDecimal monthIncome = financeService.sumAmount(incomeQuery);

        BizFinanceRecord expenseQuery = new BizFinanceRecord();
        expenseQuery.setUserId(userId);
        expenseQuery.setRecordType(BizConstants.RecordType.EXPENSE);
        expenseQuery.getParams().put("beginRecordDate", monthBegin);
        expenseQuery.getParams().put("endRecordDate", today);
        BigDecimal monthExpense = financeService.sumAmount(expenseQuery);

        vo.setMonthIncome(monthIncome);
        vo.setMonthExpense(monthExpense);
        vo.setMonthProfit(BizMathUtils.sub(monthIncome, monthExpense));

        // 待收款（不限定月份，反映整体应收）
        BizFinanceRecord receivableQuery = new BizFinanceRecord();
        receivableQuery.setUserId(userId);
        vo.setReceivable(financeService.sumReceivable(receivableQuery));

        // 进行中的计时器
        vo.setRunningTimer(workLogService.selectRunningTimer(userId));

        // 进行中项目数与我的待办
        // 微信独立用户没有部门归属，必须按 owner_id 隔离，否则会计入他人项目
        Long ownerId = BizConstants.isWxUser(userId) ? userId : null;
        vo.setRunningProjectCount(
                projectMapper.countProjectByStatus(BizConstants.ProjectStatus.RUNNING, deptId, ownerId));
        vo.setTodoTaskCount(taskMapper.countTodoTask(userId));

        // 累计工时与累计打卡条数（不加时间过滤，用于「我的」页的个人档案）
        BizWorkLog totalQuery = new BizWorkLog();
        totalQuery.setUserId(userId);
        WorkLogStatVO totalStat = workLogService.statTotal(totalQuery);
        vo.setTotalHours(BizMathUtils.secondsToHours(totalStat.getTotalSeconds()));
        vo.setTotalLogCount(totalStat.getRecordCount());

        // 近 7 天工时趋势
        BizWorkLog recentQuery = new BizWorkLog();
        recentQuery.setUserId(userId);
        recentQuery.getParams().put("beginLogDate", DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD,
                BizDateUtils.offsetDays(new Date(), -6)));
        recentQuery.getParams().put("endLogDate", today);
        vo.setRecentDailyHours(workLogService.statByDate(recentQuery));

        return vo;
    }

    @Override
    @DataScope(deptAlias = "p", userAlias = "p", userField = "owner_id")
    public List<ProjectProfitVO> selectProjectProfit(BizProject project)
    {
        List<ProjectProfitVO> list = recordMapper.selectProjectProfit(project);
        if (list == null || list.isEmpty())
        {
            return list;
        }
        for (ProjectProfitVO vo : list)
        {
            enrich(vo);
        }
        return list;
    }

    @Override
    public ProjectProfitVO selectProjectProfitDetail(Long projectId)
    {
        ProjectProfitVO vo = recordMapper.selectProjectProfitById(projectId);
        return enrich(vo);
    }

    @Override
    public List<CategoryStatVO> selectCategoryStat(BizFinanceRecord record)
    {
        return financeService.statByCategory(record);
    }

    /**
     * 补算派生指标：毛利率 / 人力成本 / 净利 / 应收 / 预算使用率
     */
    private ProjectProfitVO enrich(ProjectProfitVO vo)
    {
        if (vo == null)
        {
            return null;
        }
        BigDecimal income = BizMathUtils.nvl(vo.getIncomeTotal());
        BigDecimal cost = BizMathUtils.nvl(vo.getCostTotal());
        BigDecimal settled = BizMathUtils.nvl(vo.getSettledIncome());
        BigDecimal hours = BizMathUtils.nvl(vo.getWorkHours());
        BigDecimal billableHours = BizMathUtils.nvl(vo.getBillableHours());
        BigDecimal rate = BizMathUtils.nvl(vo.getHourRate());
        BigDecimal budget = BizMathUtils.nvl(vo.getBudgetAmount());

        BigDecimal grossProfit = BizMathUtils.sub(income, cost);
        BigDecimal laborCost = BizMathUtils.mul(hours, rate);
        BigDecimal netProfit = BizMathUtils.sub(grossProfit, laborCost);

        vo.setIncomeTotal(income);
        vo.setCostTotal(cost);
        vo.setSettledIncome(settled);
        vo.setWorkHours(hours);
        vo.setBillableHours(billableHours);
        vo.setHourRate(rate);
        vo.setBudgetAmount(budget);
        vo.setGrossProfit(grossProfit);
        vo.setGrossRate(BizMathUtils.percent(grossProfit, income));
        vo.setLaborCost(laborCost);
        vo.setNetProfit(netProfit);
        vo.setNetRate(BizMathUtils.percent(netProfit, income));
        vo.setReceivable(BizMathUtils.sub(income, settled));
        vo.setBudgetUsedRate(BizMathUtils.percent(cost, budget));
        vo.setOverBudget(budget.signum() > 0 && cost.compareTo(budget) > 0);
        return vo;
    }
}
