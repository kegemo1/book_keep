package com.ruoyi.biz.task;

import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.biz.mapper.BizWxUserMapper;
import com.ruoyi.biz.util.BizDateUtils;

/**
 * 微信注销用户物理清理任务（REQ-001）
 *
 * <p>注销档案保留 30 天后物理删除。定时任务配置（sys_job）：
 * <pre>
 *   调用目标：wxUserCleanTask.cleanCancelledWxUsers
 *   cron    ：0 30 2 * * ?（每天 02:30）
 * </pre>
 *
 * @author bookkeepingWhiz
 */
@Component("wxUserCleanTask")
public class WxUserCleanTask
{
    private static final Logger log = LoggerFactory.getLogger(WxUserCleanTask.class);

    /** 注销档案保留天数 */
    private static final int RETAIN_DAYS = 30;

    @Autowired
    private BizWxUserMapper wxUserMapper;

    public void cleanCancelledWxUsers()
    {
        Date deadline = BizDateUtils.offsetDays(new Date(), -RETAIN_DAYS);
        int rows = wxUserMapper.deleteCancelledBefore(deadline);
        if (rows > 0)
        {
            log.info("[微信注销清理] 物理删除 {} 条注销超 {} 天的用户档案（截止 {}）", rows, RETAIN_DAYS, deadline);
        }
        else
        {
            log.info("[微信注销清理] 无到期档案需要清理");
        }
    }
}
