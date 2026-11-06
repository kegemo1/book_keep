package com.ruoyi.biz.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 收支流水对象 biz_finance_record
 *
 * <p>金额恒为正数，收/支方向由 recordType 决定：
 * 净额 = SUM(CASE WHEN record_type='0' THEN amount ELSE -amount END)
 *
 * @author bookkeepingWhiz
 */
public class BizFinanceRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 流水ID */
    private Long recordId;

    /** 流水号（业务唯一，用于幂等） */
    @Excel(name = "流水号")
    private String recordNo;

    /** 记账人用户ID */
    private Long userId;

    /** 所属部门ID（冗余，用于数据权限过滤） */
    private Long deptId;

    /** 项目ID */
    private Long projectId;

    /** 任务ID */
    private Long taskId;

    /** 收支类别ID */
    private Long categoryId;

    /** 收支类型（0收入 1支出） */
    @Excel(name = "收支类型", readConverterExp = "0=收入,1=支出")
    private String recordType;

    /** 金额（元，恒为正数） */
    @Excel(name = "金额")
    private BigDecimal amount;

    /** 税额（元） */
    private BigDecimal taxAmount;

    /** 币种 */
    private String currency;

    /** 发生日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "发生日期", dateFormat = "yyyy-MM-dd")
    private Date recordDate;

    /** 结算方式（0微信 1支付宝 2银行转账 3现金 4其他） */
    @Excel(name = "结算方式", readConverterExp = "0=微信,1=支付宝,2=银行转账,3=现金,4=其他")
    private String payMethod;

    /** 结算状态（0已结清 1部分结算 2未结算） */
    @Excel(name = "结算状态", readConverterExp = "0=已结清,1=部分结算,2=未结算")
    private String settleStatus;

    /** 已结算金额（元） */
    private BigDecimal settledAmount;

    /** 第三方交易号/支付流水号 */
    private String tradeNo;

    /** 往来对象 */
    @Excel(name = "往来对象")
    private String counterparty;

    /** 发票号 */
    private String invoiceNo;

    /** 凭证图片地址 */
    private String voucherUrl;

    /** 来源（0PC端 1微信小程序） */
    private String source;

    /** 删除标志（0存在 2删除） */
    private String delFlag;

    /** 项目名称（关联查询用） */
    @Excel(name = "项目名称")
    private String projectName;

    /** 类别名称（关联查询用） */
    @Excel(name = "类别")
    private String categoryName;

    /** 记账人昵称（关联查询用） */
    @Excel(name = "记账人")
    private String userName;

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }

    public String getRecordNo() { return recordNo; }
    public void setRecordNo(String recordNo) { this.recordNo = recordNo; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Date getRecordDate() { return recordDate; }
    public void setRecordDate(Date recordDate) { this.recordDate = recordDate; }

    public String getPayMethod() { return payMethod; }
    public void setPayMethod(String payMethod) { this.payMethod = payMethod; }

    public String getSettleStatus() { return settleStatus; }
    public void setSettleStatus(String settleStatus) { this.settleStatus = settleStatus; }

    public BigDecimal getSettledAmount() { return settledAmount; }
    public void setSettledAmount(BigDecimal settledAmount) { this.settledAmount = settledAmount; }

    public String getTradeNo() { return tradeNo; }
    public void setTradeNo(String tradeNo) { this.tradeNo = tradeNo; }

    public String getCounterparty() { return counterparty; }
    public void setCounterparty(String counterparty) { this.counterparty = counterparty; }

    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }

    public String getVoucherUrl() { return voucherUrl; }
    public void setVoucherUrl(String voucherUrl) { this.voucherUrl = voucherUrl; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
}
