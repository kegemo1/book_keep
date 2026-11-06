package com.ruoyi.biz.service.impl;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.biz.constant.BizConstants;
import com.ruoyi.biz.domain.BizFinanceRecord;
import com.ruoyi.biz.domain.BizWxUser;
import com.ruoyi.biz.domain.dto.WxLoginDTO;
import com.ruoyi.biz.domain.vo.RunningTimerVO;
import com.ruoyi.biz.mapper.BizWxUserMapper;
import com.ruoyi.biz.service.IBizFinanceService;
import com.ruoyi.biz.service.IBizWxUserService;
import com.ruoyi.biz.service.IBizWorkLogService;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.http.HttpUtils;
import com.ruoyi.framework.web.service.TokenService;

/**
 * 微信用户 业务层实现
 *
 * <p>彻底分离：微信用户只落在 biz_wx_user，与 sys_user 无任何绑定关系；
 * 登录时构造影子身份签发 RuoYi 标准令牌。
 *
 * @author bookkeepingWhiz
 */
@Service
public class BizWxUserServiceImpl implements IBizWxUserService
{
    private static final Logger log = LoggerFactory.getLogger(BizWxUserServiceImpl.class);

    /** 微信 code2Session 接口 */
    private static final String CODE2SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";

    /** 身份类型固定为微信独立用户（与后台 sys 账号区分，便于前端与日志识别） */
    private static final String USER_TYPE_WX = "wx";

    @Autowired
    private BizWxUserMapper wxUserMapper;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private IBizWorkLogService workLogService;

    @Autowired
    private IBizFinanceService financeService;

    /** 小程序 AppID */
    @Value("${wx.miniapp.appid:}")
    private String appid;

    /** 小程序密钥 */
    @Value("${wx.miniapp.secret:}")
    private String secret;

    /** 是否允许为首次登录的微信自动建档（注意：仅建 biz_wx_user，绝不建 sys_user） */
    @Value("${wx.miniapp.auto-create-wx-user:true}")
    private boolean autoCreateWxUser;

    /** 令牌有效期（分钟），与 RuoYi token.expireTime 保持一致 */
    @Value("${token.expireTime:30}")
    private int expireTime;

    @Override
    public List<BizWxUser> selectWxUserList(BizWxUser wxUser)
    {
        return wxUserMapper.selectWxUserList(wxUser);
    }

    @Override
    public BizWxUser selectWxUserById(Long wxUserId)
    {
        return wxUserMapper.selectWxUserById(wxUserId);
    }

    @Override
    public BizWxUser selectByOpenid(String appid, String openid)
    {
        return wxUserMapper.selectWxUserByOpenid(appid, openid);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BizWxUser getOrCreate(String appid, String openid, String nickName, String avatarUrl)
    {
        BizWxUser exist = wxUserMapper.selectWxUserByOpenid(appid, openid);
        Date now = DateUtils.getNowDate();

        if (exist != null)
        {
            BizWxUser update = new BizWxUser();
            update.setWxUserId(exist.getWxUserId());
            update.setLastLoginTime(now);
            if (StringUtils.isNotEmpty(nickName))
            {
                update.setNickName(nickName);
                exist.setNickName(nickName);
            }
            if (StringUtils.isNotEmpty(avatarUrl))
            {
                update.setAvatarUrl(avatarUrl);
                exist.setAvatarUrl(avatarUrl);
            }
            wxUserMapper.updateWxUser(update);
            exist.setLastLoginTime(now);
            return exist;
        }

        BizWxUser entity = new BizWxUser();
        entity.setAppid(appid);
        entity.setOpenid(openid);
        entity.setNickName(StringUtils.isNotEmpty(nickName) ? nickName
                : ("微信用户" + Math.abs(openid.hashCode() % 100000)));
        entity.setAvatarUrl(StringUtils.isNotEmpty(avatarUrl) ? avatarUrl : "");
        entity.setStatus(BizConstants.STATUS_NORMAL);
        entity.setRegisterTime(now);
        entity.setLastLoginTime(now);
        entity.setDelFlag(BizConstants.DEL_FLAG_NORMAL);
        entity.setCreateBy("wx");
        entity.setCreateTime(now);
        entity.setRemark("小程序首次登录自动建档");
        wxUserMapper.insertWxUser(entity);
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateWxUser(BizWxUser wxUser)
    {
        return wxUserMapper.updateWxUser(wxUser);
    }

    @Override
    public int updateLoginInfo(Long wxUserId)
    {
        return wxUserMapper.updateLoginInfo(wxUserId, DateUtils.getNowDate());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteWxUserById(Long wxUserId)
    {
        return wxUserMapper.deleteWxUserById(wxUserId);
    }

    @Override
    public Map<String, Object> login(WxLoginDTO dto)
    {
        String openid = getOpenidByCode(dto.getCode());
        BizWxUser wxUser = wxUserMapper.selectWxUserByOpenid(appid, openid);

        if (wxUser == null)
        {
            if (!autoCreateWxUser)
            {
                throw new ServiceException("该微信用户不存在，且已关闭自动建档，请联系管理员");
            }
            wxUser = getOrCreate(appid, openid, dto.getNickName(), dto.getAvatarUrl());
        }
        else
        {
            // 已建档：刷新昵称头像与登录时间
            BizWxUser update = new BizWxUser();
            update.setWxUserId(wxUser.getWxUserId());
            update.setLastLoginTime(DateUtils.getNowDate());
            if (StringUtils.isNotEmpty(dto.getNickName()))
            {
                update.setNickName(dto.getNickName());
                wxUser.setNickName(dto.getNickName());
            }
            if (StringUtils.isNotEmpty(dto.getAvatarUrl()))
            {
                update.setAvatarUrl(dto.getAvatarUrl());
                wxUser.setAvatarUrl(dto.getAvatarUrl());
            }
            wxUserMapper.updateWxUser(update);
        }

        if (!BizConstants.STATUS_NORMAL.equals(wxUser.getStatus()))
        {
            throw new ServiceException("该微信账号已被停用，请联系管理员");
        }

        // 影子身份 → 签发 RuoYi 标准令牌
        LoginUser loginUser = buildShadowLoginUser(wxUser);
        String token = tokenService.createToken(loginUser);

        log.info("微信用户登录：wxUserId={}, openid={}", wxUser.getWxUserId(), maskOpenid(openid));
        return buildResult(token, wxUser);
    }

    @Override
    public String getOpenidByCode(String code)
    {
        if (StringUtils.isEmpty(appid) || StringUtils.isEmpty(secret))
        {
            // 本地联调模式：未配置微信参数时，用 code 派生一个稳定的伪 openid
            log.warn("未配置 wx.miniapp.appid/secret，已启用本地调试模式（请勿在生产环境使用）");
            return "dev_" + code;
        }
        String url = CODE2SESSION_URL + "?appid=" + appid + "&secret=" + secret + "&js_code=" + code
                + "&grant_type=authorization_code";
        String response = HttpUtils.sendGet(url);
        if (StringUtils.isEmpty(response))
        {
            throw new ServiceException("微信登录失败：未获取到接口响应");
        }
        JSONObject json = JSON.parseObject(response);
        Integer errcode = json.getInteger("errcode");
        if (errcode != null && errcode != 0)
        {
            log.error("微信登录失败：errcode={}, errmsg={}", errcode, json.getString("errmsg"));
            throw new ServiceException("微信登录失败：" + json.getString("errmsg"));
        }
        String openid = json.getString("openid");
        if (StringUtils.isEmpty(openid))
        {
            throw new ServiceException("微信登录失败：未获取到 openid");
        }
        return openid;
    }

    @Override
    public LoginUser buildShadowLoginUser(BizWxUser wxUser)
    {
        // 影子 SysUser：只用于承载身份与数据范围，永不落库
        SysUser shadow = new SysUser();
        shadow.setUserId(wxUser.getWxUserId());
        shadow.setUserName("wx_" + wxUser.getWxUserId());
        shadow.setNickName(StringUtils.isNotEmpty(wxUser.getNickName()) ? wxUser.getNickName() : shadow.getUserName());
        shadow.setAvatar(wxUser.getAvatarUrl());
        shadow.setDeptId(wxUser.getDeptId());
        shadow.setStatus(BizConstants.STATUS_NORMAL);
        shadow.setDelFlag(BizConstants.DEL_FLAG_NORMAL);

        // 虚拟角色：仅本人数据权限。
        // 必须携带：DataScopeAspect 在 roles 为空时 sqlString 为空 → 最终不做任何过滤（能看到全部数据）。
        SysRole selfRole = new SysRole();
        selfRole.setRoleId(0L);
        selfRole.setRoleName("微信用户");
        selfRole.setRoleKey("wx");
        selfRole.setDataScope(Constants.Dept.DATA_SCOPE_SELF);
        selfRole.setStatus(UserConstants.ROLE_NORMAL);
        selfRole.setFlag(false);
        shadow.setRoles(Collections.singletonList(selfRole));

        // 权限集合为空：小程序接口不依赖角色鉴权，统一由 @DataScope 按 user_id 隔离
        LoginUser loginUser = new LoginUser(wxUser.getWxUserId(), wxUser.getDeptId(), shadow, new HashSet<>());
        loginUser.setLoginTime(System.currentTimeMillis());
        loginUser.setExpireTime(loginUser.getLoginTime() + expireTime * 60 * 1000L);
        return loginUser;
    }

    // ------------------------------------------------------------------
    // 账号注销（REQ-001）
    // ------------------------------------------------------------------

    @Override
    public Map<String, Object> checkCancel(Long wxUserId)
    {
        BizWxUser user = wxUserMapper.selectWxUserById(wxUserId);
        if (user == null)
        {
            throw new ServiceException("账号不存在或已注销");
        }
        RunningTimerVO timer = workLogService.selectRunningTimer(wxUserId);
        BizFinanceRecord query = new BizFinanceRecord();
        query.setUserId(wxUserId);
        BigDecimal receivable = financeService.sumReceivable(query);

        Map<String, Object> data = new HashMap<>(4);
        data.put("canCancel", timer == null);
        data.put("hasRunning", timer != null);
        data.put("receivable", receivable);
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelAccount(Long wxUserId)
    {
        BizWxUser user = wxUserMapper.selectWxUserById(wxUserId);
        if (user == null)
        {
            throw new ServiceException("账号不存在或已注销");
        }
        // 硬阻断：存在进行中的计时时禁止注销（数据一致性保护，双端校验）
        RunningTimerVO timer = workLogService.selectRunningTimer(wxUserId);
        if (timer != null)
        {
            throw new ServiceException("存在进行中的计时，请先结束或丢弃后再注销");
        }
        // 注销：档案脱敏 + 身份字段置空；业务数据 user_id 保留，档案 30 天后物理清理
        wxUserMapper.cancelWxUser(wxUserId, DateUtils.getNowDate());
        log.info("微信用户注销：wxUserId={}，档案已脱敏，业务数据保留（30天后物理清理）", wxUserId);
    }

    // ------------------------------------------------------------------
    // 私有方法
    // ------------------------------------------------------------------

    private Map<String, Object> buildResult(String token, BizWxUser wxUser)
    {
        Map<String, Object> result = new HashMap<>(10);
        result.put("token", token);
        result.put("expireTime", expireTime);
        result.put("userType", USER_TYPE_WX);
        result.put("userId", wxUser.getWxUserId());
        result.put("userName", "wx_" + wxUser.getWxUserId());
        result.put("nickName", wxUser.getNickName());
        result.put("avatar", wxUser.getAvatarUrl());
        result.put("deptId", wxUser.getDeptId());
        return result;
    }

    private String maskOpenid(String openid)
    {
        if (StringUtils.isEmpty(openid) || openid.length() <= 8)
        {
            return "***";
        }
        return openid.substring(0, 4) + "****" + openid.substring(openid.length() - 4);
    }
}
