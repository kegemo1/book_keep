package com.ruoyi.biz.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 工时打卡记录对象 biz_work_log
 *
 * <p>同时承载两种录入方式：
 * <ul>
 *   <li>计时器（logType=0）：开始打点写入 logStatus=0 的记录，结束打点回填 endTime/duration 并置 logStatus=1</li>
 *   <li>手动补录（logType=1）：直接写入 logStatus=1，startTime/endTime 可为空，duration 必填</li>
 * </ul>
 *
 * @author bookkeepingWhiz
 */
public class BizWorkLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 打卡记录ID */
    private Long logId;

    /** 记录人用户ID（sys_user.user_id） */
    private Long userId;

    /** 所属部门ID（冗余，用于数据权限过滤） */
    private Long deptId;

    /** 项目ID */
    private Long projectId;

    /** 任务ID */
    private Long taskId;

    /** 业务日期（归属日期，按天统计以此为维度） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "日期", dateFormat = "yyyy-MM-dd")
    private Date logDate;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "开始时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "结束时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 时长（秒） */
    @Excel(name = "时长(秒)")
    private Integer duration;

    /** 录入方式（0计时器 1手动补录） */
    @Excel(name = "录入方式", readConverterExp = "0=计时器,1=手动补录")
    private String logType;

    /** 状态（0进行中 1已完成 2已作废） */
    @Excel(name = "状态", readConverterExp = "0=进行中,1=已完成,2=已作废")
    private String logStatus;

    /** 是否计费工时（0计费 1不计费） */
    @Excel(name = "计费工时", readConverterExp = "0=计费,1=不计费")
    private String billable;

    /** 工作内容 */
    @Excel(name = "工作内容")
    private String content;

    /** 来源（0PC端 1微信小程序） */
    private String source;

    /** 删除标志（0存在 2删除） */
    private String delFlag;

    /** 项目名称（关联查询用） */
    @Excel(name = "项目名称")
    private String projectName;

    /** 任务名称（关联查询用） */
    @Excel(name = "任务名称")
    private String taskName;

    /** 记录人昵称（关联查询用） */
    @Excel(name = "记录人")
    private String userName;

    /**
     * 时长（小时），只读计算属性，供前端展示与统计使用
     */
    public BigDecimal getDurationHour()
    {
        if (duration == null || duration <= 0)
        {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(duration).divide(new BigDecimal(3600), 2, RoundingMode.HALF_UP);
    }

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public Date getLogDate() { return logDate; }
    public void setLogDate(Date logDate) { this.logDate = logDate; }

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public String getLogType() { return logType; }
    public void setLogType(String logType) { this.logType = logType; }

    public String getLogStatus() { return logStatus; }
    public void setLogStatus(String logStatus) { this.logStatus = logStatus; }

    public String getBillable() { return billable; }
    public void setBillable(String billable) { this.billable = billable; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
}
