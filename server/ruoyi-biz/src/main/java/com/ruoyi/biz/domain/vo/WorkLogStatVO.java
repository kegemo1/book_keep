package com.ruoyi.biz.domain.vo;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 工时统计视图（按日期 / 按项目 / 按人 聚合的通用行）
 *
 * @author bookkeepingWhiz
 */
public class WorkLogStatVO
{
    /** 统计维度 ID（按日期统计时为 null，按项目统计时为项目ID，按人统计时为用户ID） */
    private Long dimId;

    /** 统计维度名称（项目名 / 用户昵称） */
    private String dimName;

    /** 统计日期（按天统计时使用） */
    private Date statDate;

    /** 总时长（秒） */
    private Long totalSeconds;

    /** 计费时长（秒） */
    private Long billableSeconds;

    /** 记录条数 */
    private Long recordCount;

    public Long getDimId() { return dimId; }
    public void setDimId(Long dimId) { this.dimId = dimId; }

    public String getDimName() { return dimName; }
    public void setDimName(String dimName) { this.dimName = dimName; }

    @JsonFormat(pattern = "yyyy-MM-dd")
    public Date getStatDate() { return statDate; }
    public void setStatDate(Date statDate) { this.statDate = statDate; }

    public Long getTotalSeconds() { return totalSeconds; }
    public void setTotalSeconds(Long totalSeconds) { this.totalSeconds = totalSeconds; }

    public Long getBillableSeconds() { return billableSeconds; }
    public void setBillableSeconds(Long billableSeconds) { this.billableSeconds = billableSeconds; }

    public Long getRecordCount() { return recordCount; }
    public void setRecordCount(Long recordCount) { this.recordCount = recordCount; }

    /** 总时长（小时），展示用 */
    public BigDecimal getTotalHours()
    {
        return toHours(totalSeconds);
    }

    /** 计费时长（小时），展示用 */
    public BigDecimal getBillableHours()
    {
        return toHours(billableSeconds);
    }

    private BigDecimal toHours(Long seconds)
    {
        if (seconds == null || seconds <= 0)
        {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(seconds).divide(new BigDecimal(3600), 2, java.math.RoundingMode.HALF_UP);
    }
}
