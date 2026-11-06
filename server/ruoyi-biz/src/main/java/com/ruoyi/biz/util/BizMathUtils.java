package com.ruoyi.biz.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 业务数值计算工具
 *
 * <p>统一金额保留 2 位、除零返回 0、null 视为 0，避免各处出现 NPE 与精度漂移。
 *
 * @author bookkeepingWhiz
 */
public class BizMathUtils
{
    /** 金额/比例小数位 */
    public static final int SCALE = 2;

    /** 百分比的基数 */
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    /** 一小时对应的秒数 */
    private static final BigDecimal SECONDS_OF_HOUR = new BigDecimal("3600");

    private BizMathUtils()
    {
    }

    /** null 安全：null 转为 0，并统一小数位 */
    public static BigDecimal nvl(BigDecimal value)
    {
        if (value == null || value.signum() == 0)
        {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /** 加法 */
    public static BigDecimal add(BigDecimal a, BigDecimal b)
    {
        return nvl(a).add(nvl(b)).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /** 减法 */
    public static BigDecimal sub(BigDecimal a, BigDecimal b)
    {
        return nvl(a).subtract(nvl(b)).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /** 乘法 */
    public static BigDecimal mul(BigDecimal a, BigDecimal b)
    {
        return nvl(a).multiply(nvl(b)).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /** 除法，除数为 0 时返回 0，避免 ArithmeticException */
    public static BigDecimal div(BigDecimal a, BigDecimal b)
    {
        if (b == null || b.signum() == 0)
        {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }
        return nvl(a).divide(b, SCALE, RoundingMode.HALF_UP);
    }

    /** 百分比：part / total * 100，total 为 0 时返回 0 */
    public static BigDecimal percent(BigDecimal part, BigDecimal total)
    {
        if (total == null || total.signum() == 0)
        {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }
        return nvl(part).multiply(HUNDRED).divide(total, SCALE, RoundingMode.HALF_UP);
    }

    /** 秒 → 小时（保留 2 位） */
    public static BigDecimal secondsToHours(Integer seconds)
    {
        if (seconds == null || seconds <= 0)
        {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }
        return new BigDecimal(seconds).divide(SECONDS_OF_HOUR, SCALE, RoundingMode.HALF_UP);
    }

    /** 秒 → 小时（Long 入参） */
    public static BigDecimal secondsToHours(Long seconds)
    {
        if (seconds == null || seconds <= 0)
        {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }
        return new BigDecimal(seconds).divide(SECONDS_OF_HOUR, SCALE, RoundingMode.HALF_UP);
    }

    /** 小时 → 秒（四舍五入取整） */
    public static int hoursToSeconds(BigDecimal hours)
    {
        if (hours == null || hours.signum() <= 0)
        {
            return 0;
        }
        return hours.multiply(SECONDS_OF_HOUR).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    /** a > b */
    public static boolean gt(BigDecimal a, BigDecimal b)
    {
        return nvl(a).compareTo(nvl(b)) > 0;
    }

    /** a >= b */
    public static boolean gte(BigDecimal a, BigDecimal b)
    {
        return nvl(a).compareTo(nvl(b)) >= 0;
    }
}
