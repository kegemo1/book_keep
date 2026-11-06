package com.ruoyi.biz.domain.dto;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * 工时手动补录参数（如「今天花 2 小时在 A 项目上」）
 *
 * @author bookkeepingWhiz
 */
public class WorkLogManualDTO
{
    /** 项目ID */
    private Long projectId;

    /** 任务ID */
    private Long taskId;

    /** 归属日期（为空取当天） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date logDate;

    /** 时长（小时，支持 2.5 这类小数） */
    @NotNull(message = "工时不能为空")
    @DecimalMin(value = "0.01", message = "工时必须大于 0")
    private BigDecimal hours;

    /** 开始时间（可空，仅当补录时段时需要） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 结束时间（可空） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 工作内容 */
    private String content;

    /** 是否计费工时（0计费 1不计费） */
    private String billable;

    /** 来源（0PC端 1微信小程序） */
    private String source;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public Date getLogDate() { return logDate; }
    public void setLogDate(Date logDate) { this.logDate = logDate; }

    public BigDecimal getHours() { return hours; }
    public void setHours(BigDecimal hours) { this.hours = hours; }

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getBillable() { return billable; }
    public void setBillable(String billable) { this.billable = billable; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
