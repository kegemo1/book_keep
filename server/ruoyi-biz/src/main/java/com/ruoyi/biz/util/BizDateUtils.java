package com.ruoyi.biz.util;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;

/**
 * 业务日期工具（基于 java.time，统一按服务器默认时区换算）
 *
 * @author bookkeepingWhiz
 */
public class BizDateUtils
{
    private BizDateUtils()
    {
    }

    /** 今天的 00:00:00 */
    public static Date todayStart()
    {
        return toDate(LocalDate.now().atStartOfDay());
    }

    /** 今天的 23:59:59 */
    public static Date todayEnd()
    {
        return toDate(LocalDate.now().atTime(LocalTime.MAX));
    }

    /** 本周一 00:00:00 */
    public static Date weekStart()
    {
        return toDate(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay());
    }

    /** 本月 1 日 00:00:00 */
    public static Date monthStart()
    {
        return toDate(LocalDate.now().withDayOfMonth(1).atStartOfDay());
    }

    /** 指定日期的 00:00:00 */
    public static Date dayStart(Date date)
    {
        return toDate(toLocalDate(date).atStartOfDay());
    }

    /** 指定日期的 23:59:59 */
    public static Date dayEnd(Date date)
    {
        return toDate(toLocalDate(date).atTime(LocalTime.MAX));
    }

    /** 指定日期的次日 00:00:00 */
    public static Date nextDayStart(Date date)
    {
        return toDate(toLocalDate(date).plusDays(1).atStartOfDay());
    }

    /** 日期偏移（天） */
    public static Date offsetDays(Date date, int days)
    {
        return toDate(toLocalDateTime(date).plusDays(days));
    }

    /** 日期偏移（月） */
    public static Date offsetMonths(Date date, int months)
    {
        return toDate(toLocalDateTime(date).plusMonths(months));
    }

    /** 是否同一天 */
    public static boolean isSameDay(Date a, Date b)
    {
        if (a == null || b == null)
        {
            return false;
        }
        return toLocalDate(a).equals(toLocalDate(b));
    }

    /** 两个日期相差的天数（to - from，按本地日期计算） */
    public static int daysBetween(Date from, Date to)
    {
        if (from == null || to == null)
        {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(toLocalDate(from), toLocalDate(to));
    }

    /** 时间差（秒），end 早于 start 时返回 0 */
    public static long secondsBetween(Date start, Date end)
    {
        if (start == null || end == null || end.before(start))
        {
            return 0L;
        }
        return (end.getTime() - start.getTime()) / 1000L;
    }

    /** 是否未来日期（按天比较） */
    public static boolean isFuture(Date date)
    {
        if (date == null)
        {
            return false;
        }
        return toLocalDate(date).isAfter(LocalDate.now());
    }

    public static Date toDate(LocalDate localDate)
    {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public static Date toDate(LocalDateTime localDateTime)
    {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static LocalDate toLocalDate(Date date)
    {
        return toLocalDateTime(date).toLocalDate();
    }

    public static LocalDateTime toLocalDateTime(Date date)
    {
        if (date instanceof java.sql.Date)
        {
            return ((java.sql.Date) date).toLocalDate().atStartOfDay();
        }
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
