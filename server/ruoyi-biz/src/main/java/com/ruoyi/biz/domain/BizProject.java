package com.ruoyi.biz.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 项目对象 biz_project
 *
 * @author bookkeepingWhiz
 */
public class BizProject extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 项目ID */
    private Long projectId;

    /** 项目编号 */
    @Excel(name = "项目编号")
    private String projectCode;

    /** 项目名称 */
    @Excel(name = "项目名称")
    private String projectName;

    /** 归属部门ID（sys_dept.dept_id） */
    private Long deptId;

    /** 项目负责人用户ID（sys_user.user_id） */
    private Long ownerId;

    /** 项目类型（字典 biz_project_type） */
    @Excel(name = "项目类型", readConverterExp = "1=自研产品,2=客户外包,3=内部事务")
    private String projectType;

    /** 客户/甲方名称 */
    @Excel(name = "客户名称")
    private String customerName;

    /** 项目状态（0进行中 1已完成 2已暂停 3已归档） */
    @Excel(name = "项目状态", readConverterExp = "0=进行中,1=已完成,2=已暂停,3=已归档")
    private String status;

    /** 开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "开始日期", dateFormat = "yyyy-MM-dd")
    private Date startDate;

    /** 计划交付日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "计划交付日期", dateFormat = "yyyy-MM-dd")
    private Date planEndDate;

    /** 实际结项日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "结项日期", dateFormat = "yyyy-MM-dd")
    private Date endDate;

    /** 预算金额（元） */
    @Excel(name = "预算金额")
    private BigDecimal budgetAmount;

    /** 默认时薪（元/小时） */
    @Excel(name = "默认时薪")
    private BigDecimal hourRate;

    /** 显示顺序 */
    private Integer orderNum;

    /** 删除标志（0存在 2删除） */
    private String delFlag;

    /** 归属部门名称（关联查询用） */
    private String deptName;

    /** 负责人昵称（关联查询用） */
    private String ownerName;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public String getProjectType() { return projectType; }
    public void setProjectType(String projectType) { this.projectType = projectType; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getPlanEndDate() { return planEndDate; }
    public void setPlanEndDate(Date planEndDate) { this.planEndDate = planEndDate; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    public BigDecimal getBudgetAmount() { return budgetAmount; }
    public void setBudgetAmount(BigDecimal budgetAmount) { this.budgetAmount = budgetAmount; }

    public BigDecimal getHourRate() { return hourRate; }
    public void setHourRate(BigDecimal hourRate) { this.hourRate = hourRate; }

    public Integer getOrderNum() { return orderNum; }
    public void setOrderNum(Integer orderNum) { this.orderNum = orderNum; }

    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
}
