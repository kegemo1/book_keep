package com.ruoyi.biz.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.biz.domain.BizWorkLog;
import com.ruoyi.biz.domain.vo.WorkLogStatVO;

/**
 * 工时打卡 数据层
 *
 * @author bookkeepingWhiz
 */
public interface BizWorkLogMapper
{
    /**
     * 查询打卡记录列表（支持 @DataScope 部门数据权限，别名 w）
     */
    List<BizWorkLog> selectWorkLogList(BizWorkLog log);

    /**
     * 根据记录ID查询
     */
    BizWorkLog selectWorkLogById(Long logId);

    /**
     * 查询用户当前进行中的计时器（唯一约束保证最多一条）
     */
    BizWorkLog selectRunningTimer(Long userId);

    /**
     * 查询超时未结束的计时器（定时任务清理用）
     */
    List<BizWorkLog> selectOvertimeRunning(@Param("deadline") Date deadline);

    /**
     * 新增打卡记录
     */
    int insertWorkLog(BizWorkLog log);

    /**
     * 修改打卡记录
     */
    int updateWorkLog(BizWorkLog log);

    /**
     * 结束计时：仅允许「本人 + 进行中」的记录被结束
     */
    int finishTimer(BizWorkLog log);

    /**
     * 变更记录状态（作废/恢复）
     */
    int updateWorkLogStatus(@Param("logId") Long logId, @Param("logStatus") String logStatus,
            @Param("updateBy") String updateBy);

    /**
     * 逻辑删除
     */
    int deleteWorkLogById(@Param("logId") Long logId, @Param("updateBy") String updateBy);

    /**
     * 批量逻辑删除
     */
    int deleteWorkLogByIds(@Param("logIds") Long[] logIds, @Param("updateBy") String updateBy);

    /**
     * 按日期分组统计工时
     */
    List<WorkLogStatVO> selectDailyStat(BizWorkLog query);

    /**
     * 按项目分组统计工时
     */
    List<WorkLogStatVO> selectProjectStat(BizWorkLog query);

    /**
     * 按人分组统计工时
     */
    List<WorkLogStatVO> selectUserStat(BizWorkLog query);

    /**
     * 合计统计（无分组）
     */
    WorkLogStatVO selectTotalStat(BizWorkLog query);

    /**
     * 按任务分组统计工时（定时任务回写 actual_hours 用）
     */
    List<WorkLogStatVO> selectTaskStatByRecord(BizWorkLog query);

    /**
     * 统计某项目累计工时（秒）
     */
    Long sumSecondsByProject(Long projectId);
}
