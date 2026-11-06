package com.ruoyi.biz.domain.dto;

/**
 * 计时器开始打点参数
 *
 * @author bookkeepingWhiz
 */
public class TimerStartDTO
{
    /** 项目ID（可为空，表示未归类的内部事务） */
    private Long projectId;

    /** 任务ID */
    private Long taskId;

    /** 工作内容 */
    private String content;

    /** 是否计费工时（0计费 1不计费） */
    private String billable;

    /** 来源（0PC端 1微信小程序） */
    private String source;

    /** 是否允许自动切换：true 时若已有进行中的计时器则先结算它（跨项目切换场景） */
    private Boolean autoSwitch = Boolean.FALSE;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getBillable() { return billable; }
    public void setBillable(String billable) { this.billable = billable; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Boolean getAutoSwitch() { return autoSwitch; }
    public void setAutoSwitch(Boolean autoSwitch) { this.autoSwitch = autoSwitch; }
}
