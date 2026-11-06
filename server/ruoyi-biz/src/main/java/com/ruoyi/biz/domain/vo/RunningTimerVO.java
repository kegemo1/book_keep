package com.ruoyi.biz.domain.vo;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 进行中的计时器视图
 *
 * @author bookkeepingWhiz
 */
public class RunningTimerVO
{
    /** 打卡记录ID */
    private Long logId;

    /** 项目ID */
    private Long projectId;

    /** 项目名称 */
    private String projectName;

    /** 任务ID */
    private Long taskId;

    /** 任务名称 */
    private String taskName;

    /** 工作内容 */
    private String content;

    /** 是否计费工时 */
    private String billable;

    /** 归属日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date logDate;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 已运行秒数（服务端计算，前端仅做展示递增） */
    private Long runningSeconds;

    /** 是否已超时（超过单次计时上限，前端提示用户确认） */
    private Boolean overtime;

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getBillable() { return billable; }
    public void setBillable(String billable) { this.billable = billable; }

    public Date getLogDate() { return logDate; }
    public void setLogDate(Date logDate) { this.logDate = logDate; }

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public Long getRunningSeconds() { return runningSeconds; }
    public void setRunningSeconds(Long runningSeconds) { this.runningSeconds = runningSeconds; }

    public Boolean getOvertime() { return overtime; }
    public void setOvertime(Boolean overtime) { this.overtime = overtime; }
}
