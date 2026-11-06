package com.ruoyi.biz.domain.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 计时器结束打点参数
 *
 * @author bookkeepingWhiz
 */
public class TimerStopDTO
{
    /** 打卡记录ID */
    @NotNull(message = "打卡记录ID不能为空")
    private Long logId;

    /** 结束时间（为空则取服务端当前时间，防止小程序端时钟漂移） */
    private java.util.Date endTime;

    /** 工作内容（结束时补充） */
    private String content;

    /** 是否计费工时（0计费 1不计费） */
    private String billable;

    /** 是否丢弃该次计时（true 时作废记录，用于误点开始） */
    private Boolean discard = Boolean.FALSE;

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }

    public java.util.Date getEndTime() { return endTime; }
    public void setEndTime(java.util.Date endTime) { this.endTime = endTime; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getBillable() { return billable; }
    public void setBillable(String billable) { this.billable = billable; }

    public Boolean getDiscard() { return discard; }
    public void setDiscard(Boolean discard) { this.discard = discard; }
}
