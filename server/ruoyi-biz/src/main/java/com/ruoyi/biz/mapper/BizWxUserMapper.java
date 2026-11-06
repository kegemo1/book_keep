package com.ruoyi.biz.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.biz.domain.BizWxUser;

/**
 * 微信用户 数据层
 *
 * @author bookkeepingWhiz
 */
public interface BizWxUserMapper
{
    /**
     * 查询微信用户列表
     * （params.cancelled == '1' 时查询已注销档案，否则只查正常档案）
     */
    List<BizWxUser> selectWxUserList(BizWxUser wxUser);

    /**
     * 根据主键查询
     */
    BizWxUser selectWxUserById(Long wxUserId);

    /**
     * 根据 appid + openid 查询（微信身份的唯一标识）
     */
    BizWxUser selectWxUserByOpenid(@Param("appid") String appid, @Param("openid") String openid);

    /**
     * 新增微信用户
     */
    int insertWxUser(BizWxUser wxUser);

    /**
     * 修改微信用户资料
     */
    int updateWxUser(BizWxUser wxUser);

    /**
     * 更新最近登录时间
     */
    int updateLoginInfo(@Param("wxUserId") Long wxUserId, @Param("lastLoginTime") Date lastLoginTime);

    /**
     * 账号注销：del_flag=2、openid/unionid 置空、昵称头像手机号脱敏，记录注销时间
     */
    int cancelWxUser(@Param("wxUserId") Long wxUserId, @Param("cancelTime") Date cancelTime);

    /**
     * 物理删除注销超过指定期限的用户档案（定时任务调用）
     */
    int deleteCancelledBefore(@Param("deadline") Date deadline);

    /**
     * 物理删除
     * @param wxUserId
     * @return
     */
    int deleteWxUserById(Long wxUserId);
}
