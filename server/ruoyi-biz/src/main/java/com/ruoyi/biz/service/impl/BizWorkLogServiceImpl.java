package com.ruoyi.biz.service.impl;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.biz.constant.BizConstants;
import com.ruoyi.biz.domain.BizProject;
import com.ruoyi.biz.domain.BizTask;
import com.ruoyi.biz.domain.BizWorkLog;
import com.ruoyi.biz.domain.dto.TimerStartDTO;
import com.ruoyi.biz.domain.dto.TimerStopDTO;
import com.ruoyi.biz.domain.dto.WorkLogManualDTO;
import com.ruoyi.biz.domain.vo.RunningTimerVO;
import com.ruoyi.biz.domain.vo.TimerStopResultVO;
import com.ruoyi.biz.domain.vo.WorkLogStatVO;
import com.ruoyi.biz.mapper.BizWorkLogMapper;
import com.ruoyi.biz.service.IBizProjectService;
import com.ruoyi.biz.service.IBizTaskService;
import com.ruoyi.biz.service.IBizWorkLogService;
import com.ruoyi.biz.util.BizDateUtils;
import com.ruoyi.biz.util.BizMathUtils;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;

/**
 * 工时打卡 业务层实现
 *
 * <p>核心约束：
 * <ol>
 *   <li>同一用户同时只允许存在一个「进行中」的计时器（Redis 拦截 + 数据库唯一索引兜底）</li>
 *   <li>时长一律由服务端根据起止时间计算，不信任客户端上报</li>
 *   <li>跨零点自动按自然日拆分为多条记录，保证「按天统计」口径不失真</li>
 * </ol>
 *
 * @author bookkeepingWhiz
 */
@Service
public class BizWorkLogServiceImpl implements IBizWorkLogService
{
    private static final Logger log = LoggerFactory.getLogger(BizWorkLogServiceImpl.class);

    @Autowired
    private BizWorkLogMapper workLogMapper;

    @Autowired
    private IBizProjectService projectService;

    @Autowired
    private IBizTaskService taskService;

    @Autowired
    private RedisCache redisCache;

    @Override
    @DataScope(deptAlias = "w", userAlias = "w")
    public List<BizWorkLog> selectWorkLogList(BizWorkLog query)
    {
        return workLogMapper.selectWorkLogList(query);
    }

    @Override
    public BizWorkLog selectWorkLogById(Long logId)
    {
        return workLogMapper.selectWorkLogById(logId);
    }

    @Override
    public RunningTimerVO selectRunningTimer(Long userId)
    {
        BizWorkLog running = workLogMapper.selectRunningTimer(userId);
        return running == null ? null : toRunningVO(running);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RunningTimerVO startTimer(TimerStartDTO dto, LoginUser loginUser)
    {
        Long userId = loginUser.getUserId();

        // 1. 已有进行中的计时器：按 autoSwitch 决定「自动结算」还是「直接拒绝」
        BizWorkLog running = workLogMapper.selectRunningTimer(userId);
        if (running != null)
        {
            if (Boolean.TRUE.equals(dto.getAutoSwitch()))
            {
                doFinish(running, new Date(), null, loginUser, "切换任务时自动结算");
            }
            else
            {
                throw new ServiceException("已有进行中的计时任务「" + describe(running) + "」，请先结束或选择切换任务");
            }
        }

        // 2. 业务校验：项目/任务是否存在、是否有权打卡
        checkProjectAndTask(dto.getProjectId(), dto.getTaskId(), userId);

        // 3. 落库
        BizWorkLog entity = new BizWorkLog();
        Date now = new Date();
        entity.setUserId(userId);
        entity.setDeptId(loginUser.getDeptId());
        entity.setProjectId(dto.getProjectId());
        entity.setTaskId(dto.getTaskId());
        entity.setLogDate(now);
        entity.setStartTime(now);
        entity.setDuration(0);
        entity.setLogType(BizConstants.WorkLogType.TIMER);
        entity.setLogStatus(BizConstants.WorkLogStatus.RUNNING);
        entity.setBillable(StringUtils.isEmpty(dto.getBillable()) ? BizConstants.Billable.YES : dto.getBillable());
        entity.setContent(dto.getContent());
        entity.setSource(StringUtils.isEmpty(dto.getSource()) ? BizConstants.Source.PC : dto.getSource());
        entity.setDelFlag(BizConstants.DEL_FLAG_NORMAL);
        entity.setCreateBy(loginUser.getUsername());
        entity.setCreateTime(now);
        try
        {
            workLogMapper.insertWorkLog(entity);
        }
        catch (DuplicateKeyException e)
        {
            // 命中 uk_running_user：并发重复点击「开始」
            throw new ServiceException("已有进行中的计时任务，请勿重复开始");
        }

        // 4. Redis 作为第一道拦截（DB 唯一索引是最后一道防线）
        redisCache.setCacheObject(BizConstants.RUNNING_TIMER_KEY + userId, entity.getLogId(),
                (int) BizConstants.RUNNING_TIMER_EXPIRE_SECONDS, TimeUnit.SECONDS);

        log.info("计时开始：userId={}, logId={}, projectId={}, taskId={}", userId, entity.getLogId(),
                dto.getProjectId(), dto.getTaskId());
        return toRunningVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TimerStopResultVO stopTimer(TimerStopDTO dto, LoginUser loginUser)
    {
        BizWorkLog running = workLogMapper.selectWorkLogById(dto.getLogId());
        if (running == null)
        {
            throw new ServiceException("打卡记录不存在或已删除");
        }
        if (!BizConstants.WorkLogStatus.RUNNING.equals(running.getLogStatus()))
        {
            throw new ServiceException("该计时任务已结束，无需重复操作");
        }
        // 越权保护：只有本人（或管理员）能结束该计时器
        if (!loginUser.getUserId().equals(running.getUserId()) && !SecurityUtils.isAdmin(loginUser.getUserId()))
        {
            throw new ServiceException("无权结束他人的计时任务");
        }

        // 丢弃：用于误点开始，记录保留但作废，便于审计
        if (Boolean.TRUE.equals(dto.getDiscard()))
        {
            workLogMapper.updateWorkLogStatus(running.getLogId(), BizConstants.WorkLogStatus.CANCELLED,
                    loginUser.getUsername());
            clearRunningKey(running.getUserId());
            TimerStopResultVO vo = new TimerStopResultVO();
            vo.setLogId(running.getLogId());
            vo.setStartTime(running.getStartTime());
            vo.setEndTime(new Date());
            vo.setTotalSeconds(0);
            vo.setTotalHours(BizMathUtils.nvl(null));
            vo.setCrossDay(Boolean.FALSE);
            vo.setSegmentCount(0);
            vo.setTruncated(Boolean.FALSE);
            vo.setDiscarded(Boolean.TRUE);
            return vo;
        }

        Date end = dto.getEndTime() == null ? new Date() : dto.getEndTime();
        if (end.before(running.getStartTime()))
        {
            throw new ServiceException("结束时间不能早于开始时间");
        }
        return doFinish(running, end, dto, loginUser, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BizWorkLog addManualLog(WorkLogManualDTO dto, LoginUser loginUser)
    {
        Date logDate = dto.getLogDate() == null ? new Date() : dto.getLogDate();

        // 1. 日期规则：不允许未来，且限制回溯天数
        if (BizDateUtils.isFuture(logDate))
        {
            throw new ServiceException("不能补录未来日期的工时");
        }
        int backDays = BizDateUtils.daysBetween(logDate, new Date());
        if (backDays > BizConstants.MANUAL_BACKFILL_MAX_DAYS)
        {
            throw new ServiceException("仅支持补录近 " + BizConstants.MANUAL_BACKFILL_MAX_DAYS + " 天的工时，更早请走管理员调整");
        }

        // 2. 时长规则：给了起止时间就按时间差算，只给了小时数就按小时数换算
        int duration;
        if (dto.getStartTime() != null && dto.getEndTime() != null)
        {
            duration = (int) BizDateUtils.secondsBetween(dto.getStartTime(), dto.getEndTime());
            if (duration <= 0)
            {
                throw new ServiceException("结束时间必须晚于开始时间");
            }
        }
        else
        {
            duration = BizMathUtils.hoursToSeconds(dto.getHours());
        }
        if (duration <= 0)
        {
            throw new ServiceException("工时必须大于 0");
        }
        if (duration > BizConstants.MAX_MANUAL_DURATION_SECONDS)
        {
            throw new ServiceException("单条补录不能超过 24 小时");
        }

        // 3. 项目/任务校验
        checkProjectAndTask(dto.getProjectId(), dto.getTaskId(), loginUser.getUserId());

        // 4. 单日累计上限：防止把 20 小时误录成 200 小时
        long existedSeconds = sumSeconds(loginUser.getUserId(), logDate, null);
        if (existedSeconds + duration > BizConstants.SECONDS_OF_DAY)
        {
            throw new ServiceException("当日累计工时将超过 24 小时，请核对补录时长");
        }

        // 5. 落库（补录直接是已完成状态）
        BizWorkLog entity = new BizWorkLog();
        Date now = new Date();
        entity.setUserId(loginUser.getUserId());
        entity.setDeptId(loginUser.getDeptId());
        entity.setProjectId(dto.getProjectId());
        entity.setTaskId(dto.getTaskId());
        entity.setLogDate(logDate);
        entity.setStartTime(dto.getStartTime());
        entity.setEndTime(dto.getEndTime());
        entity.setDuration(duration);
        entity.setLogType(BizConstants.WorkLogType.MANUAL);
        entity.setLogStatus(BizConstants.WorkLogStatus.FINISHED);
        entity.setBillable(StringUtils.isEmpty(dto.getBillable()) ? BizConstants.Billable.YES : dto.getBillable());
        entity.setContent(dto.getContent());
        entity.setSource(StringUtils.isEmpty(dto.getSource()) ? BizConstants.Source.PC : dto.getSource());
        entity.setDelFlag(BizConstants.DEL_FLAG_NORMAL);
        entity.setCreateBy(loginUser.getUsername());
        entity.setCreateTime(now);
        workLogMapper.insertWorkLog(entity);

        syncTaskHours(entity.getTaskId());
        log.info("手动补录工时：userId={}, logId={}, duration={}s", loginUser.getUserId(), entity.getLogId(), duration);
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateWorkLog(BizWorkLog log)
    {
        BizWorkLog db = workLogMapper.selectWorkLogById(log.getLogId());
        if (db == null)
        {
            throw new ServiceException("打卡记录不存在或已删除");
        }
        if (!SecurityUtils.getUserId().equals(db.getUserId()) && !SecurityUtils.isAdmin())
        {
            throw new ServiceException("只能修改自己的打卡记录");
        }
        if (log.getStartTime() != null && log.getEndTime() != null)
        {
            int seconds = (int) BizDateUtils.secondsBetween(log.getStartTime(), log.getEndTime());
            if (seconds <= 0)
            {
                throw new ServiceException("结束时间必须晚于开始时间");
            }
            log.setDuration(seconds);
        }
        log.setUpdateBy(SecurityUtils.getUsername());
        log.setUpdateTime(DateUtils.getNowDate());
        int rows = workLogMapper.updateWorkLog(log);
        syncTaskHours(db.getTaskId());
        if (log.getTaskId() != null && !log.getTaskId().equals(db.getTaskId()))
        {
            syncTaskHours(log.getTaskId());
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cancelWorkLog(Long logId, String reason)
    {
        BizWorkLog db = workLogMapper.selectWorkLogById(logId);
        if (db == null)
        {
            throw new ServiceException("打卡记录不存在或已删除");
        }
        if (!SecurityUtils.getUserId().equals(db.getUserId()) && !SecurityUtils.isAdmin())
        {
            throw new ServiceException("只能作废自己的打卡记录");
        }
        if (BizConstants.WorkLogStatus.CANCELLED.equals(db.getLogStatus()))
        {
            return 0;
        }
        int rows = workLogMapper.updateWorkLogStatus(logId, BizConstants.WorkLogStatus.CANCELLED,
                SecurityUtils.getUsername());
        if (BizConstants.WorkLogStatus.RUNNING.equals(db.getLogStatus()))
        {
            clearRunningKey(db.getUserId());
        }
        syncTaskHours(db.getTaskId());
        log.info("作废打卡记录：logId={}, reason={}", logId, reason);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int restoreWorkLog(Long logId)
    {
        BizWorkLog db = workLogMapper.selectWorkLogById(logId);
        if (db == null)
        {
            throw new ServiceException("打卡记录不存在或已删除");
        }
        if (!BizConstants.WorkLogStatus.CANCELLED.equals(db.getLogStatus()))
        {
            throw new ServiceException("仅可恢复已作废的记录");
        }
        int rows = workLogMapper.updateWorkLogStatus(logId, BizConstants.WorkLogStatus.FINISHED,
                SecurityUtils.getUsername());
        syncTaskHours(db.getTaskId());
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteWorkLogById(Long logId)
    {
        BizWorkLog db = workLogMapper.selectWorkLogById(logId);
        if (db == null)
        {
            throw new ServiceException("打卡记录不存在或已删除");
        }
        if (!SecurityUtils.getUserId().equals(db.getUserId()) && !SecurityUtils.isAdmin())
        {
            throw new ServiceException("只能删除自己的打卡记录");
        }
        int rows = workLogMapper.deleteWorkLogById(logId, SecurityUtils.getUsername());
        if (BizConstants.WorkLogStatus.RUNNING.equals(db.getLogStatus()))
        {
            clearRunningKey(db.getUserId());
        }
        syncTaskHours(db.getTaskId());
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteWorkLogByIds(Long[] logIds)
    {
        if (logIds == null || logIds.length == 0)
        {
            return 0;
        }
        for (Long logId : logIds)
        {
            BizWorkLog db = workLogMapper.selectWorkLogById(logId);
            if (db != null && BizConstants.WorkLogStatus.RUNNING.equals(db.getLogStatus()))
            {
                clearRunningKey(db.getUserId());
            }
        }
        return workLogMapper.deleteWorkLogByIds(logIds, SecurityUtils.getUsername());
    }

    @Override
    @DataScope(deptAlias = "w", userAlias = "w")
    public List<WorkLogStatVO> statByDate(BizWorkLog query)
    {
        return workLogMapper.selectDailyStat(query);
    }

    @Override
    @DataScope(deptAlias = "w", userAlias = "w")
    public List<WorkLogStatVO> statByProject(BizWorkLog query)
    {
        return workLogMapper.selectProjectStat(query);
    }

    @Override
    @DataScope(deptAlias = "w", userAlias = "w")
    public List<WorkLogStatVO> statByUser(BizWorkLog query)
    {
        return workLogMapper.selectUserStat(query);
    }

    @Override
    public WorkLogStatVO statTotal(BizWorkLog query)
    {
        WorkLogStatVO stat = workLogMapper.selectTotalStat(query);
        return stat == null ? new WorkLogStatVO() : stat;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanOvertimeTimer()
    {
        // 超过单次计时上限仍未结束的记录，视为「忘记点结束」
        Date deadline = new Date(System.currentTimeMillis() - BizConstants.MAX_TIMER_DURATION_SECONDS * 1000L);
        List<BizWorkLog> list = workLogMapper.selectOvertimeRunning(deadline);
        if (list == null || list.isEmpty())
        {
            return 0;
        }
        int rows = 0;
        for (BizWorkLog running : list)
        {
            try
            {
                Date end = new Date(running.getStartTime().getTime()
                        + BizConstants.MAX_TIMER_DURATION_SECONDS * 1000L);
                doFinish(running, end, null, null, "系统自动结束：超过单次计时上限仍未打点");
                rows++;
            }
            catch (Exception e)
            {
                log.error("自动结束超时计时器失败，logId={}", running.getLogId(), e);
            }
        }
        return rows;
    }

    // ------------------------------------------------------------------
    // 私有方法
    // ------------------------------------------------------------------

    /**
     * 结束计时并落库：跨天按自然日拆分，返回本次合计时长
     *
     * @param running   进行中的记录
     * @param end       结束时间
     * @param dto       可选的结束参数（补充内容/计费标记）
     * @param loginUser 操作人，定时任务自动结束时传 null
     * @param remark    追加备注
     */
    private TimerStopResultVO doFinish(BizWorkLog running, Date end, TimerStopDTO dto, LoginUser loginUser,
            String remark)
    {
        long rawSeconds = BizDateUtils.secondsBetween(running.getStartTime(), end);
        boolean truncated = false;
        if (rawSeconds > BizConstants.MAX_TIMER_DURATION_SECONDS)
        {
            rawSeconds = BizConstants.MAX_TIMER_DURATION_SECONDS;
            end = new Date(running.getStartTime().getTime() + rawSeconds * 1000L);
            truncated = true;
        }
        if (rawSeconds < 1)
        {
            // 极端情况下（开始后立即结束）至少记 1 秒，避免出现 0 时长记录
            rawSeconds = 1L;
            end = new Date(running.getStartTime().getTime() + 1000L);
        }

        String operator = loginUser == null ? running.getCreateBy() : loginUser.getUsername();
        String content = dto != null && StringUtils.isNotEmpty(dto.getContent()) ? dto.getContent()
                : running.getContent();
        String billable = dto != null && StringUtils.isNotEmpty(dto.getBillable()) ? dto.getBillable()
                : (StringUtils.isEmpty(running.getBillable()) ? BizConstants.Billable.YES : running.getBillable());

        // 按自然日切分，得到 [起始毫秒, 结束毫秒] 片段
        List<long[]> segments = splitByDay(running.getStartTime().getTime(), end.getTime());
        int totalSeconds = 0;
        for (int i = 0; i < segments.size(); i++)
        {
            long[] seg = segments.get(i);
            int segSeconds = (int) ((seg[1] - seg[0]) / 1000L);
            if (segSeconds <= 0)
            {
                continue;
            }
            totalSeconds += segSeconds;

            if (i == 0)
            {
                // 首个片段：结束原记录
                BizWorkLog update = new BizWorkLog();
                update.setLogId(running.getLogId());
                update.setUserId(running.getUserId());
                update.setEndTime(new Date(seg[1]));
                update.setDuration(segSeconds);
                update.setLogStatus(BizConstants.WorkLogStatus.FINISHED);
                update.setContent(content);
                update.setBillable(billable);
                update.setUpdateBy(operator);
                String merged = mergeRemark(remark, truncated ? "超过单次计时上限，已自动截断" : null);
                if (StringUtils.isNotEmpty(merged))
                {
                    update.setRemark(merged);
                }
                int rows = workLogMapper.finishTimer(update);
                if (rows == 0)
                {
                    throw new ServiceException("计时任务状态已变化，请刷新后重试");
                }
            }
            else
            {
                // 后续片段：新增一条归属到次日的计时记录
                BizWorkLog segment = new BizWorkLog();
                Date segStart = new Date(seg[0]);
                segment.setUserId(running.getUserId());
                segment.setDeptId(running.getDeptId());
                segment.setProjectId(running.getProjectId());
                segment.setTaskId(running.getTaskId());
                segment.setLogDate(segStart);
                segment.setStartTime(segStart);
                segment.setEndTime(new Date(seg[1]));
                segment.setDuration(segSeconds);
                segment.setLogType(BizConstants.WorkLogType.TIMER);
                segment.setLogStatus(BizConstants.WorkLogStatus.FINISHED);
                segment.setBillable(billable);
                segment.setContent(content);
                segment.setSource(running.getSource());
                segment.setDelFlag(BizConstants.DEL_FLAG_NORMAL);
                segment.setCreateBy(operator);
                segment.setCreateTime(DateUtils.getNowDate());
                segment.setRemark("跨天自动拆分自记录 #" + running.getLogId());
                workLogMapper.insertWorkLog(segment);
            }
        }

        clearRunningKey(running.getUserId());
        syncTaskHours(running.getTaskId());

        TimerStopResultVO vo = new TimerStopResultVO();
        vo.setLogId(running.getLogId());
        vo.setLogDate(running.getLogDate());
        vo.setStartTime(running.getStartTime());
        vo.setEndTime(end);
        vo.setTotalSeconds(totalSeconds);
        vo.setTotalHours(BizMathUtils.secondsToHours(totalSeconds));
        vo.setCrossDay(segments.size() > 1);
        vo.setSegmentCount(segments.size());
        vo.setTruncated(truncated);
        vo.setDiscarded(Boolean.FALSE);
        if (running.getProjectId() != null)
        {
            BizProject project = projectService.selectProjectById(running.getProjectId());
            vo.setProjectName(project == null ? null : project.getProjectName());
        }
        if (running.getTaskId() != null)
        {
            BizTask task = taskService.selectTaskById(running.getTaskId());
            vo.setTaskName(task == null ? null : task.getTaskName());
        }
        return vo;
    }

    /**
     * 按自然日切分时间段，返回若干 [startMillis, endMillis]
     */
    private List<long[]> splitByDay(long startMillis, long endMillis)
    {
        List<long[]> segments = new ArrayList<>();
        long cursor = startMillis;
        while (cursor < endMillis)
        {
            LocalDate day = Instant.ofEpochMilli(cursor).atZone(ZoneId.systemDefault()).toLocalDate();
            long dayEndMillis = BizDateUtils.toDate(day.atTime(LocalTime.MAX)).getTime();
            long segEnd = Math.min(dayEndMillis, endMillis);
            segments.add(new long[] { cursor, segEnd });
            cursor = segEnd + 1L;
        }
        if (segments.isEmpty())
        {
            segments.add(new long[] { startMillis, endMillis });
        }
        return segments;
    }

    /**
     * 项目/任务合法性校验（含项目成员权限）
     */
    private void checkProjectAndTask(Long projectId, Long taskId, Long userId)
    {
        if (projectId != null)
        {
            BizProject project = projectService.selectProjectById(projectId);
            if (project == null)
            {
                throw new ServiceException("项目不存在或已删除");
            }
            if (BizConstants.ProjectStatus.ARCHIVED.equals(project.getStatus()))
            {
                throw new ServiceException("项目「" + project.getProjectName() + "」已归档，不能继续打卡");
            }
            if (!projectService.hasProjectPermission(projectId, userId))
            {
                throw new ServiceException("您不是该项目成员，无权在该项目下打卡");
            }
        }
        if (taskId != null)
        {
            taskService.checkTaskBelongProject(taskId, projectId);
            BizTask task = taskService.selectTaskById(taskId);
            if (task != null && BizConstants.TaskStatus.CANCELED.equals(task.getStatus()))
            {
                throw new ServiceException("任务「" + task.getTaskName() + "」已取消，不能打卡");
            }
        }
    }

    /**
     * 汇总某用户某天的已完成工时（秒）
     */
    private long sumSeconds(Long userId, Date logDate, Long projectId)
    {
        BizWorkLog query = new BizWorkLog();
        query.setUserId(userId);
        query.setProjectId(projectId);
        // selectTotalStat 只认 params 里的日期区间，实体上的 logDate 不参与查询
        if (logDate != null)
        {
            String day = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, logDate);
            query.getParams().put("beginLogDate", day);
            query.getParams().put("endLogDate", day);
        }
        WorkLogStatVO stat = workLogMapper.selectTotalStat(query);
        if (stat == null || stat.getTotalSeconds() == null)
        {
            return 0L;
        }
        return stat.getTotalSeconds();
    }

    /** 回写任务实际工时，失败不影响主流程 */
    private void syncTaskHours(Long taskId)
    {
        if (taskId == null)
        {
            return;
        }
        try
        {
            taskService.syncActualHours(taskId);
        }
        catch (Exception e)
        {
            log.warn("回写任务实际工时失败，taskId={}", taskId, e);
        }
    }

    private RunningTimerVO toRunningVO(BizWorkLog entity)
    {
        RunningTimerVO vo = new RunningTimerVO();
        vo.setLogId(entity.getLogId());
        vo.setProjectId(entity.getProjectId());
        vo.setTaskId(entity.getTaskId());
        vo.setContent(entity.getContent());
        vo.setBillable(entity.getBillable());
        vo.setLogDate(entity.getLogDate());
        vo.setStartTime(entity.getStartTime());
        long seconds = BizDateUtils.secondsBetween(entity.getStartTime(), new Date());
        vo.setRunningSeconds(seconds);
        vo.setOvertime(seconds > BizConstants.MAX_TIMER_DURATION_SECONDS);
        if (entity.getProjectId() != null)
        {
            BizProject project = projectService.selectProjectById(entity.getProjectId());
            vo.setProjectName(project == null ? null : project.getProjectName());
        }
        if (entity.getTaskId() != null)
        {
            BizTask task = taskService.selectTaskById(entity.getTaskId());
            vo.setTaskName(task == null ? null : task.getTaskName());
        }
        return vo;
    }

    private String describe(BizWorkLog running)
    {
        if (StringUtils.isNotEmpty(running.getContent()))
        {
            return running.getContent();
        }
        return running.getProjectId() == null ? "未归类任务" : ("项目 #" + running.getProjectId());
    }

    private String mergeRemark(String a, String b)
    {
        if (StringUtils.isEmpty(a))
        {
            return b;
        }
        if (StringUtils.isEmpty(b))
        {
            return a;
        }
        return a + "；" + b;
    }

    private void clearRunningKey(Long userId)
    {
        if (userId != null)
        {
            redisCache.deleteObject(BizConstants.RUNNING_TIMER_KEY + userId);
        }
    }
}
