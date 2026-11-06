package com.ruoyi.biz.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 微信用户对象 biz_wx_user
 *
 * <p>独立身份体系：微信用户不落 sys_user，只在本表建档；
 * 登录时由服务端构造「影子 SysUser」签发 RuoYi 标准令牌。
 *
 * @author bookkeepingWhiz
 */
public class BizWxUser extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 微信用户ID（从 9000000000 起，与 sys_user 主键段隔离） */
    private Long wxUserId;

    /** 小程序AppID */
    private String appid;

    /** 微信openid */
    @JsonIgnore
    private String openid;

    /** 微信unionid */
    @JsonIgnore
    private String unionid;

    /** 微信昵称 */
    @Excel(name = "微信昵称")
    private String nickName;

    /** 微信头像地址 */
    private String avatarUrl;

    /** 手机号 */
    private String phone;

    /** 可选归属部门 */
    private Long deptId;

    /** 状态（0正常 1停用） */
    private String status;

    /** 首次登录（建档）时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date registerTime;

    /** 最近一次登录时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastLoginTime;

    /** 注销时间（注销30天后由定时任务物理清理） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date cancelTime;

    /** 删除标志（0存在 2已注销/删除） */
    private String delFlag;

    public Long getWxUserId() { return wxUserId; }
    public void setWxUserId(Long wxUserId) { this.wxUserId = wxUserId; }

    public String getAppid() { return appid; }
    public void setAppid(String appid) { this.appid = appid; }

    public String getOpenid() { return openid; }
    public void setOpenid(String openid) { this.openid = openid; }

    public String getUnionid() { return unionid; }
    public void setUnionid(String unionid) { this.unionid = unionid; }

    public String getNickName() { return nickName; }
    public void setNickName(String nickName) { this.nickName = nickName; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getRegisterTime() { return registerTime; }
    public void setRegisterTime(Date registerTime) { this.registerTime = registerTime; }

    public Date getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(Date lastLoginTime) { this.lastLoginTime = lastLoginTime; }

    public Date getCancelTime() { return cancelTime; }
    public void setCancelTime(Date cancelTime) { this.cancelTime = cancelTime; }

    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
}
