package com.ruoyi.biz.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 项目成员对象 biz_project_member
 *
 * @author bookkeepingWhiz
 */
public class BizProjectMember extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 成员记录ID */
    private Long memberId;

    /** 项目ID */
    private Long projectId;

    /** 用户ID */
    private Long userId;

    /** 项目角色（0负责人 1成员 2观察者） */
    private String roleType;

    /** 成员时薪（元/小时，0表示取项目默认时薪） */
    private BigDecimal hourRate;

    /** 加入日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date joinDate;

    /** 退出日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date quitDate;

    /** 状态（0正常 1停用） */
    private String status;

    /** 删除标志（0存在 2删除） */
    private String delFlag;

    /** 项目名称（关联查询用） */
    private String projectName;

    /** 用户账号（关联查询用） */
    private String userName;

    /** 用户昵称（关联查询用） */
    private String nickName;

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getRoleType() { return roleType; }
    public void setRoleType(String roleType) { this.roleType = roleType; }

    public BigDecimal getHourRate() { return hourRate; }
    public void setHourRate(BigDecimal hourRate) { this.hourRate = hourRate; }

    public Date getJoinDate() { return joinDate; }
    public void setJoinDate(Date joinDate) { this.joinDate = joinDate; }

    public Date getQuitDate() { return quitDate; }
    public void setQuitDate(Date quitDate) { this.quitDate = quitDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getNickName() { return nickName; }
    public void setNickName(String nickName) { this.nickName = nickName; }
}
