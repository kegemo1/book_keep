package com.ruoyi.biz.domain.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 微信小程序登录参数
 *
 * @author bookkeepingWhiz
 */
public class WxLoginDTO
{
    /** wx.login 返回的临时登录凭证 code */
    @NotBlank(message = "code不能为空")
    private String code;

    /** 微信昵称（用户授权后前端回传，用于首登建档） */
    private String nickName;

    /** 微信头像地址 */
    private String avatarUrl;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getNickName() { return nickName; }
    public void setNickName(String nickName) { this.nickName = nickName; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}
