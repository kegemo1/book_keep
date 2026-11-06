package com.ruoyi.biz.domain.vo;

import java.math.BigDecimal;

/**
 * 项目盈亏核算视图
 *
 * <p>核算口径：
 * 毛利 = 收入 - 直接成本；人力成本 = 工时 × 时薪；净利 = 毛利 - 人力成本
 *
 * @author bookkeepingWhiz
 */
public class ProjectProfitVO
{
    /** 项目ID */
    private Long projectId;

    /** 项目名称 */
    private String projectName;

    /** 项目状态 */
    private String status;

    /** 客户名称 */
    private String customerName;

    /** 预算金额 */
    private BigDecimal budgetAmount;

    /** 收入合计 */
    private BigDecimal incomeTotal;

    /** 直接成本合计（硬件/服务等） */
    private BigDecimal costTotal;

    /** 毛利 = 收入 - 直接成本 */
    private BigDecimal grossProfit;

    /** 毛利率（%，收入为 0 时返回 null） */
    private BigDecimal grossRate;

    /** 已结算金额（收入侧） */
    private BigDecimal settledIncome;

    /** 应收未收 = 收入 - 已结算 */
    private BigDecimal receivable;

    /** 总工时（小时） */
    private BigDecimal workHours;

    /** 计费工时（小时） */
    private BigDecimal billableHours;

    /** 时薪 */
    private BigDecimal hourRate;

    /** 人力成本 = 总工时 × 时薪 */
    private BigDecimal laborCost;

    /** 净利润 = 毛利 - 人力成本 */
    private BigDecimal netProfit;

    /** 净利率（%） */
    private BigDecimal netRate;

    /** 预算使用率（%）= 直接成本 / 预算 */
    private BigDecimal budgetUsedRate;

    /** 是否超预算 */
    private Boolean overBudget;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public BigDecimal getBudgetAmount() { return budgetAmount; }
    public void setBudgetAmount(BigDecimal budgetAmount) { this.budgetAmount = budgetAmount; }

    public BigDecimal getIncomeTotal() { return incomeTotal; }
    public void setIncomeTotal(BigDecimal incomeTotal) { this.incomeTotal = incomeTotal; }

    public BigDecimal getCostTotal() { return costTotal; }
    public void setCostTotal(BigDecimal costTotal) { this.costTotal = costTotal; }

    public BigDecimal getGrossProfit() { return grossProfit; }
    public void setGrossProfit(BigDecimal grossProfit) { this.grossProfit = grossProfit; }

    public BigDecimal getGrossRate() { return grossRate; }
    public void setGrossRate(BigDecimal grossRate) { this.grossRate = grossRate; }

    public BigDecimal getSettledIncome() { return settledIncome; }
    public void setSettledIncome(BigDecimal settledIncome) { this.settledIncome = settledIncome; }

    public BigDecimal getReceivable() { return receivable; }
    public void setReceivable(BigDecimal receivable) { this.receivable = receivable; }

    public BigDecimal getWorkHours() { return workHours; }
    public void setWorkHours(BigDecimal workHours) { this.workHours = workHours; }

    public BigDecimal getBillableHours() { return billableHours; }
    public void setBillableHours(BigDecimal billableHours) { this.billableHours = billableHours; }

    public BigDecimal getHourRate() { return hourRate; }
    public void setHourRate(BigDecimal hourRate) { this.hourRate = hourRate; }

    public BigDecimal getLaborCost() { return laborCost; }
    public void setLaborCost(BigDecimal laborCost) { this.laborCost = laborCost; }

    public BigDecimal getNetProfit() { return netProfit; }
    public void setNetProfit(BigDecimal netProfit) { this.netProfit = netProfit; }

    public BigDecimal getNetRate() { return netRate; }
    public void setNetRate(BigDecimal netRate) { this.netRate = netRate; }

    public BigDecimal getBudgetUsedRate() { return budgetUsedRate; }
    public void setBudgetUsedRate(BigDecimal budgetUsedRate) { this.budgetUsedRate = budgetUsedRate; }

    public Boolean getOverBudget() { return overBudget; }
    public void setOverBudget(Boolean overBudget) { this.overBudget = overBudget; }
}
