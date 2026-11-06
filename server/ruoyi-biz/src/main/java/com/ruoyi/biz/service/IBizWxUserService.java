package com.ruoyi.biz.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.biz.domain.BizWxUser;
import com.ruoyi.biz.domain.dto.WxLoginDTO;
import com.ruoyi.common.core.domain.model.LoginUser;

/**
 * 微信用户 业务层
 *
 * <p><b>彻底分离策略：微信用户与后台用户（sys_user）是两套互不相干的账号体系，不做任何绑定。</b>
 * <ul>
 *   <li>后台用户：sys_user，走 RuoYi 原生 /login，用于管理端</li>
 *   <li>微信用户：biz_wx_user，走 /wx/auth/login，用于小程序，绝不写入 sys_user</li>
 * </ul>
 *
 * @author bookkeepingWhiz
 */
public interface IBizWxUserService
{
    /**
     * 查询微信用户列表
     */
    List<BizWxUser> selectWxUserList(BizWxUser wxUser);

    /**
     * 根据主键查询
     */
    BizWxUser selectWxUserById(Long wxUserId);

    /**
     * 根据 appid + openid 查询
     */
    BizWxUser selectByOpenid(String appid, String openid);

    /**
     * 获取微信用户，不存在则建档（只写 biz_wx_user，不碰 sys_user）
     */
    BizWxUser getOrCreate(String appid, String openid, String nickName, String avatarUrl);

    /**
     * 修改微信用户资料
     */
    int updateWxUser(BizWxUser wxUser);

    /**
     * 更新最近登录时间
     */
    int updateLoginInfo(Long wxUserId);

    /**
     * 逻辑删除微信用户
     */
    int deleteWxUserById(Long wxUserId);

    /**
     * 小程序登录：code 换 openid → 命中档案（或建档）→ 签发令牌
     *
     * @return 含 token / userId / userType 等信息的 Map
     */
    Map<String, Object> login(WxLoginDTO dto);

    /**
     * 用 code 换取 openid（微信 code2Session 封装）
     */
    String getOpenidByCode(String code);

    /**
     * 构造「影子身份」用于签发令牌（含仅本人数据权限的虚拟角色）
     */
    LoginUser buildShadowLoginUser(BizWxUser wxUser);

    /**
     * 注销前检查：账号是否存在、是否有进行中计时、应收款金额
     *
     * @return {canCancel, hasRunning, receivable}
     */
    Map<String, Object> checkCancel(Long wxUserId);

    /**
     * 执行账号注销（REQ-001）：
     * biz_wx_user 逻辑删除（del_flag=2）、openid/unionid 置空、档案脱敏、记录注销时间；
     * 业务数据（工时/流水/项目）的 user_id 保留，档案 30 天后由定时任务物理清理。
     *
     * @param wxUserId 当前登录的微信用户ID
     */
    void cancelAccount(Long wxUserId);
}
