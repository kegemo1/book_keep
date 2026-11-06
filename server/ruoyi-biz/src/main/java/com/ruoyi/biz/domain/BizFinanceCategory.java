package com.ruoyi.biz.domain;

import java.util.ArrayList;
import java.util.List;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 收支类别对象 biz_finance_category
 *
 * @author bookkeepingWhiz
 */
public class BizFinanceCategory extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 类别ID */
    private Long categoryId;

    /** 父类别ID（0为顶级） */
    private Long parentId;

    /** 祖级列表 */
    private String ancestors;

    /** 类别名称 */
    @Excel(name = "类别名称")
    private String categoryName;

    /** 适用类型（0收入 1支出） */
    @Excel(name = "适用类型", readConverterExp = "0=收入,1=支出")
    private String categoryType;

    /** 是否系统预置（Y是 N否） */
    private String sysDefault;

    /** 显示顺序 */
    private Integer orderNum;

    /** 状态（0正常 1停用） */
    private String status;

    /** 删除标志（0存在 2删除） */
    private String delFlag;

    /** 父类别名称（关联查询用） */
    private String parentName;

    /** 子类别 */
    private List<BizFinanceCategory> children = new ArrayList<>();

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public String getAncestors() { return ancestors; }
    public void setAncestors(String ancestors) { this.ancestors = ancestors; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getCategoryType() { return categoryType; }
    public void setCategoryType(String categoryType) { this.categoryType = categoryType; }

    public String getSysDefault() { return sysDefault; }
    public void setSysDefault(String sysDefault) { this.sysDefault = sysDefault; }

    public Integer getOrderNum() { return orderNum; }
    public void setOrderNum(Integer orderNum) { this.orderNum = orderNum; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    public String getParentName() { return parentName; }
    public void setParentName(String parentName) { this.parentName = parentName; }

    public List<BizFinanceCategory> getChildren() { return children; }
    public void setChildren(List<BizFinanceCategory> children) { this.children = children; }
}
