package com.ruoyi.biz.service;

import java.util.List;
import com.ruoyi.biz.domain.BizWorkLog;
import com.ruoyi.biz.domain.dto.TimerStartDTO;
import com.ruoyi.biz.domain.dto.TimerStopDTO;
import com.ruoyi.biz.domain.dto.WorkLogManualDTO;
import com.ruoyi.biz.domain.vo.RunningTimerVO;
import com.ruoyi.biz.domain.vo.TimerStopResultVO;
import com.ruoyi.biz.domain.vo.WorkLogStatVO;
import com.ruoyi.common.core.domain.model.LoginUser;

/**
 * 工时打卡 业务层
 *
 * @author bookkeepingWhiz
 */
public interface IBizWorkLogService
{
    /**
     * 查询打卡记录列表
     */
    List<BizWorkLog> selectWorkLogList(BizWorkLog log);

    /**
     * 根据记录ID查询
     */
    BizWorkLog selectWorkLogById(Long logId);

    /**
     * 查询当前进行中的计时器（无则返回 null）
     */
    RunningTimerVO selectRunningTimer(Long userId);

    /**
     * 开始计时：写入一条 log_status=0 的记录。
     * 若已存在进行中的计时器：autoSwitch=true 时自动结算上一条，否则抛出业务异常。
     */
    RunningTimerVO startTimer(TimerStartDTO dto, LoginUser loginUser);

    /**
     * 结束计时：回填结束时间与时长；跨零点自动按天拆分为多条记录。
     */
    TimerStopResultVO stopTimer(TimerStopDTO dto, LoginUser loginUser);

    /**
     * 手动补录工时（如「今天花 2 小时在 A 项目上」）
     */
    BizWorkLog addManualLog(WorkLogManualDTO dto, LoginUser loginUser);

    /**
     * 修改打卡记录（仅本人或管理员，且不允许越权改他人记录）
     */
    int updateWorkLog(BizWorkLog log);

    /**
     * 作废记录（保留审计痕迹）
     */
    int cancelWorkLog(Long logId, String reason);

    /**
     * 恢复被作废的记录
     */
    int restoreWorkLog(Long logId);

    /**
     * 逻辑删除记录
     */
    int deleteWorkLogById(Long logId);

    /**
     * 批量逻辑删除记录
     */
    int deleteWorkLogByIds(Long[] logIds);

    /**
     * 按日期统计工时
     */
    List<WorkLogStatVO> statByDate(BizWorkLog query);

    /**
     * 按项目统计工时
     */
    List<WorkLogStatVO> statByProject(BizWorkLog query);

    /**
     * 按人统计工时
     */
    List<WorkLogStatVO> statByUser(BizWorkLog query);

    /**
     * 合计工时（小时）
     */
    WorkLogStatVO statTotal(BizWorkLog query);

    /**
     * 清理超时未结束的计时器（定时任务调用）：按单次上限截断并自动结束
     *
     * @return 处理条数
     */
    int cleanOvertimeTimer();
}
