package com.ruoyi.biz.domain.vo;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 结束计时后的结果视图
 *
 * @author bookkeepingWhiz
 */
public class TimerStopResultVO
{
    /** 打卡记录ID（主记录） */
    private Long logId;

    /** 项目名称 */
    private String projectName;

    /** 任务名称 */
    private String taskName;

    /** 归属日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date logDate;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 本次总时长（秒，跨天拆分的合计） */
    private Integer totalSeconds;

    /** 本次总时长（小时） */
    private BigDecimal totalHours;

    /** 是否跨天拆分 */
    private Boolean crossDay;

    /** 拆分后生成的记录条数 */
    private Integer segmentCount;

    /** 是否因超过单次上限被自动截断 */
    private Boolean truncated;

    /** 是否已作废（用户选择丢弃） */
    private Boolean discarded;

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public Date getLogDate() { return logDate; }
    public void setLogDate(Date logDate) { this.logDate = logDate; }

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    public Integer getTotalSeconds() { return totalSeconds; }
    public void setTotalSeconds(Integer totalSeconds) { this.totalSeconds = totalSeconds; }

    public BigDecimal getTotalHours() { return totalHours; }
    public void setTotalHours(BigDecimal totalHours) { this.totalHours = totalHours; }

    public Boolean getCrossDay() { return crossDay; }
    public void setCrossDay(Boolean crossDay) { this.crossDay = crossDay; }

    public Integer getSegmentCount() { return segmentCount; }
    public void setSegmentCount(Integer segmentCount) { this.segmentCount = segmentCount; }

    public Boolean getTruncated() { return truncated; }
    public void setTruncated(Boolean truncated) { this.truncated = truncated; }

    public Boolean getDiscarded() { return discarded; }
    public void setDiscarded(Boolean discarded) { this.discarded = discarded; }
}
