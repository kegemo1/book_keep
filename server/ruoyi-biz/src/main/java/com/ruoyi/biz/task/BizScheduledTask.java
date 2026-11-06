package com.ruoyi.biz.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.biz.service.IBizTaskService;
import com.ruoyi.biz.service.IBizWorkLogService;

/**
 * 业务定时任务
 *
 * <p>在 RuoYi「系统监控 → 定时任务」中新增两个任务即可：
 * <ul>
 *   <li>调用目标：<code>bizTask.cleanOvertimeTimer()</code>，Cron <code>0 0/30 * * * ?</code>（每 30 分钟）</li>
 *   <li>调用目标：<code>bizTask.syncTaskActualHours()</code>，Cron <code>0 10 0 * * ?</code>（每天 00:10）</li>
 * </ul>
 *
 * @author bookkeepingWhiz
 */
@Component("bizTask")
public class BizScheduledTask
{
    private static final Logger log = LoggerFactory.getLogger(BizScheduledTask.class);

    @Autowired
    private IBizWorkLogService workLogService;

    @Autowired
    private IBizTaskService taskService;

    /**
     * 清理超时未结束的计时器：
     * 用户忘记点「结束」时，按单次计时上限截断并自动结束，避免工时被无限累加
     */
    public void cleanOvertimeTimer()
    {
        try
        {
            int rows = workLogService.cleanOvertimeTimer();
            log.info("【定时任务】超时计时器清理完成，处理 {} 条", rows);
        }
        catch (Exception e)
        {
            log.error("【定时任务】超时计时器清理失败", e);
        }
    }

    /**
     * 回写任务实际工时：把 biz_work_log 的工时汇总到 biz_task.actual_hours
     */
    public void syncTaskActualHours()
    {
        try
        {
            int rows = taskService.syncAllTaskActualHours();
            log.info("【定时任务】任务实际工时回写完成，更新 {} 条", rows);
        }
        catch (Exception e)
        {
            log.error("【定时任务】任务实际工时回写失败", e);
        }
    }
}
