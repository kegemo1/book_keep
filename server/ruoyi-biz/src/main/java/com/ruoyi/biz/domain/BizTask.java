package com.ruoyi.biz.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 任务对象 biz_task
 *
 * @author bookkeepingWhiz
 */
public class BizTask extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 任务ID */
    private Long taskId;

    /** 所属项目ID */
    private Long projectId;

    /** 父任务ID（0为顶级任务） */
    private Long parentId;

    /** 祖级列表 */
    private String ancestors;

    /** 任务名称 */
    @Excel(name = "任务名称")
    private String taskName;

    /** 执行人用户ID */
    private Long assigneeId;

    /** 优先级（0高 1中 2低） */
    @Excel(name = "优先级", readConverterExp = "0=高,1=中,2=低")
    private String priority;

    /** 任务状态（0待处理 1进行中 2已完成 3已取消） */
    @Excel(name = "任务状态", readConverterExp = "0=待处理,1=进行中,2=已完成,3=已取消")
    private String status;

    /** 计划开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date planStartTime;

    /** 计划完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date planEndTime;

    /** 预估工时（小时） */
    @Excel(name = "预估工时")
    private BigDecimal estimateHours;

    /** 实际工时（小时，由打卡记录汇总回写） */
    @Excel(name = "实际工时")
    private BigDecimal actualHours;

    /** 进度百分比 */
    private Integer progress;

    /** 显示顺序 */
    private Integer orderNum;

    /** 删除标志（0存在 2删除） */
    private String delFlag;

    /** 所属项目名称（关联查询用） */
    private String projectName;

    /** 执行人昵称（关联查询用） */
    private String assigneeName;

    /** 子任务（树形展示用） */
    private List<BizTask> children = new ArrayList<>();

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public String getAncestors() { return ancestors; }
    public void setAncestors(String ancestors) { this.ancestors = ancestors; }

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getPlanStartTime() { return planStartTime; }
    public void setPlanStartTime(Date planStartTime) { this.planStartTime = planStartTime; }

    public Date getPlanEndTime() { return planEndTime; }
    public void setPlanEndTime(Date planEndTime) { this.planEndTime = planEndTime; }

    public BigDecimal getEstimateHours() { return estimateHours; }
    public void setEstimateHours(BigDecimal estimateHours) { this.estimateHours = estimateHours; }

    public BigDecimal getActualHours() { return actualHours; }
    public void setActualHours(BigDecimal actualHours) { this.actualHours = actualHours; }

    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }

    public Integer getOrderNum() { return orderNum; }
    public void setOrderNum(Integer orderNum) { this.orderNum = orderNum; }

    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getAssigneeName() { return assigneeName; }
    public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }

    public List<BizTask> getChildren() { return children; }
    public void setChildren(List<BizTask> children) { this.children = children; }
}
