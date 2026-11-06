package com.ruoyi.biz.constant;

import java.math.BigDecimal;

/**
 * 业务常量
 *
 * @author bookkeepingWhiz
 */
public class BizConstants
{
    /** 逻辑删除：存在 */
    public static final String DEL_FLAG_NORMAL = "0";

    /** 逻辑删除：已删除 */
    public static final String DEL_FLAG_DELETED = "2";

    /** 状态：正常 / 启用 */
    public static final String STATUS_NORMAL = "0";

    /** 计时器进行中标记的 Redis Key 前缀（完整 Key = 前缀 + userId） */
    public static final String RUNNING_TIMER_KEY = "biz:worklog:running:";

    /** 计时器 Key 的过期时间（秒）：异常退出后避免锁被永久占用 */
    public static final long RUNNING_TIMER_EXPIRE_SECONDS = 12 * 3600L;

    /** 单次计时任务的最长时长（秒），超过视为「忘记点结束」 */
    public static final int MAX_TIMER_DURATION_SECONDS = 12 * 3600;

    /** 手动补录单条的最长时长（秒） */
    public static final int MAX_MANUAL_DURATION_SECONDS = 24 * 3600;

    /** 手动补录允许回溯的最大天数 */
    public static final int MANUAL_BACKFILL_MAX_DAYS = 31;

    /** 收支流水号前缀 */
    public static final String RECORD_NO_PREFIX = "BK";

    /** 单笔金额上限（元） */
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999.99");

    /** 一天的秒数 */
    public static final int SECONDS_OF_DAY = 24 * 3600;

    /**
     * 微信独立用户的 ID 起始值。
     * 业务表的 user_id 会混存两类用户：sys_user.user_id（系统账号，通常 1~1xxx）与
     * biz_wx_user.wx_user_id（微信独立用户，从 9000000000 起），两者主键段永不重叠。
     */
    public static final long WX_USER_ID_START = 9000000000L;

    /**
     * 判断某个 user_id 是否为「微信独立用户」（而非 sys_user 账号）
     */
    public static boolean isWxUser(Long userId)
    {
        return userId != null && userId.longValue() >= WX_USER_ID_START;
    }

    /**
     * 判断某个 user_id 是否为「系统账号」（sys_user）
     */
    public static boolean isSysUser(Long userId)
    {
        return userId != null && userId.longValue() < WX_USER_ID_START;
    }

    /** 小时换算的秒基数 */
    public static final BigDecimal SECONDS_OF_HOUR = new BigDecimal(3600);

    /** 工时状态：0进行中 1已完成 2已作废 */
    public interface WorkLogStatus
    {
        String RUNNING = "0";
        String FINISHED = "1";
        String CANCELLED = "2";
    }

    /** 工时录入方式：0计时器 1手动补录 */
    public interface WorkLogType
    {
        String TIMER = "0";
        String MANUAL = "1";
    }

    /** 收支类型：0收入 1支出 */
    public interface RecordType
    {
        String INCOME = "0";
        String EXPENSE = "1";
    }

    /** 结算状态：0已结清 1部分结算 2未结算 */
    public interface SettleStatus
    {
        String SETTLED = "0";
        String PARTIAL = "1";
        String UNSETTLED = "2";
    }

    /** 项目状态：0进行中 1已完成 2已暂停 3已归档 */
    public interface ProjectStatus
    {
        String RUNNING = "0";
        String FINISHED = "1";
        String PAUSED = "2";
        String ARCHIVED = "3";
    }

    /** 任务状态：0待处理 1进行中 2已完成 3已取消 */
    public interface TaskStatus
    {
        String TODO = "0";
        String DOING = "1";
        String DONE = "2";
        String CANCELED = "3";
    }

    /** 是否计费工时：0计费 1不计费 */
    public interface Billable
    {
        String YES = "0";
        String NO = "1";
    }

    /** 数据来源：0PC端 1微信小程序 */
    public interface Source
    {
        String PC = "0";
        String MINIAPP = "1";
    }
}
