package com.ruoyi.biz.domain.vo;

import java.math.BigDecimal;

/**
 * 收支类别汇总视图
 *
 * @author bookkeepingWhiz
 */
public class CategoryStatVO
{
    /** 类别ID */
    private Long categoryId;

    /** 类别名称 */
    private String categoryName;

    /** 收支类型（0收入 1支出） */
    private String categoryType;

    /** 金额合计 */
    private BigDecimal totalAmount;

    /** 流水条数 */
    private Long recordCount;

    /** 占比（%，由服务层计算） */
    private BigDecimal ratio;

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getCategoryType() { return categoryType; }
    public void setCategoryType(String categoryType) { this.categoryType = categoryType; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public Long getRecordCount() { return recordCount; }
    public void setRecordCount(Long recordCount) { this.recordCount = recordCount; }

    public BigDecimal getRatio() { return ratio; }
    public void setRatio(BigDecimal ratio) { this.ratio = ratio; }
}
