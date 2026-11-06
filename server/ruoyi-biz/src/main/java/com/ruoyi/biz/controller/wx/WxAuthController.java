package com.ruoyi.biz.controller.wx;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.biz.domain.BizWxUser;
import com.ruoyi.biz.domain.dto.WxLoginDTO;
import com.ruoyi.biz.service.IBizStatService;
import com.ruoyi.biz.service.IBizWxUserService;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.service.TokenService;
import jakarta.validation.Valid;

/**
 * 小程序 · 登录与个人资料
 *
 * <p>微信用户与后台用户彻底分离：本控制器只服务 biz_wx_user，不读写 sys_user，
 * 也不提供任何账号绑定能力。
 *
 * @author bookkeepingWhiz
 */
@RestController
@RequestMapping("/wx/auth")
public class WxAuthController extends BaseController
{
    @Autowired
    private IBizWxUserService wxUserService;

    @Autowired
    private IBizStatService statService;

    @Autowired
    private TokenService tokenService;

    /**
     * 微信一键登录：wx.login 拿到 code 后调用，换取系统令牌
     */
    @Anonymous
    @PostMapping("/login")
    public AjaxResult login(@Valid @RequestBody WxLoginDTO dto)
    {
        Map<String, Object> result = wxUserService.login(dto);
        return AjaxResult.success("登录成功", result);
    }

    /**
     * 当前微信用户资料 + 概览统计（「我的」页一次拿全，减少请求数）
     */
    @GetMapping("/info")
    public AjaxResult info()
    {
        Long wxUserId = getUserId();
        BizWxUser wxUser = wxUserService.selectWxUserById(wxUserId);
        if (wxUser == null)
        {
            throw new ServiceException("微信账号不存在");
        }
        // 脱敏：openid/unionid 在实体上已用 @JsonIgnore 屏蔽
        AjaxResult ajax = AjaxResult.success();
        ajax.put("wxUser", wxUser);
        ajax.put("userType", "wx");
        ajax.put("stat", statService.dashboard(wxUserId, getDeptId()));
        return ajax;
    }

    /**
     * 修改个人资料（微信昵称 / 头像）
     *
     * <p>头像应传已上传后的 http 地址：小程序端先用 chooseAvatar 拿到本地临时文件，
     * 再调用 /common/upload 换取长期地址后提交，否则服务端保存的是无法访问的临时路径。
     */
    @PutMapping("/profile")
    public AjaxResult updateProfile(@RequestBody BizWxUser body)
    {
        Long wxUserId = getUserId();
        BizWxUser db = wxUserService.selectWxUserById(wxUserId);
        if (db == null)
        {
            throw new ServiceException("微信账号不存在");
        }

        BizWxUser update = new BizWxUser();
        update.setWxUserId(wxUserId);
        if (StringUtils.isNotEmpty(body.getNickName()))
        {
            update.setNickName(body.getNickName());
            db.setNickName(body.getNickName());
        }
        if (StringUtils.isNotEmpty(body.getAvatarUrl()))
        {
            update.setAvatarUrl(body.getAvatarUrl());
            db.setAvatarUrl(body.getAvatarUrl());
        }
        wxUserService.updateWxUser(update);
        return AjaxResult.success("已保存", db);
    }

    /**
     * 退出登录（服务端无状态，主要由客户端清除本地 token）
     */
    @PostMapping("/logout")
    public AjaxResult logout()
    {
        return AjaxResult.success("已退出");
    }

    // ------------------------------------------------------------------
    // 账号注销（REQ-001）
    // ------------------------------------------------------------------

    /**
     * 注销前检查：是否有进行中计时（阻断项）、应收款金额（强提醒项）
     */
    @GetMapping("/cancel/check")
    public AjaxResult cancelCheck()
    {
        return AjaxResult.success(wxUserService.checkCancel(getUserId()));
    }

    /**
     * 账号注销（REQ-001）
     *
     * <p>动作：biz_wx_user 逻辑删除（del_flag=2）、openid/unionid 置空、档案脱敏、记录注销时间；
     * 业务数据 user_id 保留，档案 30 天后由定时任务物理清理。注销成功后当前令牌立即失效。
     *
     * @param body confirmed：有应收款时的用户二次确认标记
     */
    @PostMapping("/cancel")
    public AjaxResult cancel(@RequestBody(required = false) Map<String, Object> body)
    {
        boolean confirmed = body != null && Boolean.TRUE.equals(body.get("confirmed"));
        Map<String, Object> check = wxUserService.checkCancel(getUserId());

        if (Boolean.TRUE.equals(check.get("hasRunning")))
        {
            return AjaxResult.error("存在进行中的计时，请先结束或丢弃后再注销");
        }
        BigDecimal receivable = (BigDecimal) check.get("receivable");
        if (receivable != null && receivable.compareTo(BigDecimal.ZERO) > 0 && !confirmed)
        {
            AjaxResult warn = AjaxResult.warn("您有未收款 ¥" + receivable + "，注销后无法追讨，请再次确认");
            warn.put("needConfirm", true);
            warn.put("receivable", receivable);
            return warn;
        }

        wxUserService.cancelAccount(getUserId());

        // 注销后当前令牌立即失效，客户端清除本地登录态
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser != null)
        {
            tokenService.delLoginUser(loginUser.getToken());
        }
        return AjaxResult.success("账号已注销");
    }
}
