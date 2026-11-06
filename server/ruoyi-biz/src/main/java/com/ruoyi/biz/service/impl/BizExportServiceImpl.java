package com.ruoyi.biz.service.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.ruoyi.biz.domain.BizFinanceRecord;
import com.ruoyi.biz.domain.BizWorkLog;
import com.ruoyi.biz.service.IBizExportService;
import com.ruoyi.biz.service.IBizFinanceService;
import com.ruoyi.biz.service.IBizWorkLogService;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.http.HttpUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * 个人数据导出 业务层实现（REQ-002）
 *
 * <p>技术说明：项目已内置 poi 5.5.1（RuoYi ExcelUtil 同源），直接用 XSSFWorkbook
 * 生成两个 Sheet，避免引入 EasyExcel 造成 poi 版本冲突；导出效果一致。
 *
 * <p>防滥用：Redis 按用户按天限流（默认 10 次/天）；操作留痕由控制层 @Log 写入 sys_oper_log。
 *
 * @author bookkeepingWhiz
 */
@Service
public class BizExportServiceImpl implements IBizExportService
{
    private static final Logger log = LoggerFactory.getLogger(BizExportServiceImpl.class);

    /** 导出文件目录（profile 下） */
    private static final String EXPORT_DIR = "export";

    /** 每用户每日导出上限 */
    private static final int DAILY_LIMIT = 10;

    /** 结算方式映射（与小程序端保持一致） */
    private static final String[] PAY_METHODS = {"微信", "支付宝", "银行转账", "现金", "其他"};

    /** 打卡状态映射 */
    private static final String[] LOG_STATUS = {"进行中", "已完成", "已作废"};

    /** 导出文件保留天数（超过由下次导出顺带清理） */
    private static final long RETAIN_MILLIS = 3L * 24 * 3600 * 1000;

    @Autowired
    private IBizWorkLogService workLogService;

    @Autowired
    private IBizFinanceService financeService;

    @Autowired
    private RedisCache redisCache;

    @Override
    public Map<String, Object> exportUserData(Long userId, String scope, String beginDate, String endDate)
    {
        checkRateLimit(userId);
        String[] range = resolveRange(scope, beginDate, endDate);

        // 查询打卡记录
        BizWorkLog logQuery = new BizWorkLog();
        logQuery.setUserId(userId);
        if (range[0] != null)
        {
            logQuery.getParams().put("beginLogDate", range[0]);
            logQuery.getParams().put("endLogDate", range[1]);
        }
        List<BizWorkLog> logs = workLogService.selectWorkLogList(logQuery);
        if (logs == null)
        {
            logs = Collections.emptyList();
        }

        // 查询收支流水
        BizFinanceRecord finQuery = new BizFinanceRecord();
        finQuery.setUserId(userId);
        if (range[0] != null)
        {
            finQuery.getParams().put("beginRecordDate", range[0]);
            finQuery.getParams().put("endRecordDate", range[1]);
        }
        List<BizFinanceRecord> records = financeService.selectRecordList(finQuery);
        if (records == null)
        {
            records = Collections.emptyList();
        }

        if (logs.isEmpty() && records.isEmpty())
        {
            throw new ServiceException("所选范围内没有可导出的数据");
        }

        // 顺带清理 3 天前的旧导出文件
        cleanOldExports();

        // 生成文件（profile/export/）
        String dirPath = RuoYiConfig.getProfile() + File.separator + EXPORT_DIR;
        File dir = new File(dirPath);
        if (!dir.exists() && !dir.mkdirs())
        {
            throw new ServiceException("导出目录创建失败：" + dirPath);
        }
        String stamp = DateUtils.dateTimeNow();
        String xlsxName = "export_" + stamp + ".xlsx";
        String jsonName = "export_" + stamp + ".json";

        writeExcel(new File(dir, xlsxName), logs, records);
        writeJson(new File(dir, jsonName), logs, records, scope, range[0], range[1]);
        log.info("数据导出：userId={}, scope={}, 打卡 {} 条, 流水 {} 条", userId, scope, logs.size(), records.size());

        Map<String, Object> result = new HashMap<>(8);
        result.put("logCount", logs.size());
        result.put("recordCount", records.size());
        result.put("xlsxFileName", xlsxName);
        result.put("jsonFileName", jsonName);
        result.put("exportTime", DateUtils.dateTimeNow());
        return result;
    }

    // ------------------------------------------------------------------
    // 私有方法
    // ------------------------------------------------------------------

    /**
     * 限流：每用户每天最多导出 DAILY_LIMIT 次（Redis 计数）
     */
    private void checkRateLimit(Long userId)
    {
        String key = "wx_export_limit:" + userId + ":" + DateUtils.dateTimeNow("yyyyMMdd");
        Integer count = redisCache.getCacheObject(key);
        int current = count == null ? 0 : count;
        if (current >= DAILY_LIMIT)
        {
            throw new ServiceException("今日导出次数已达上限（" + DAILY_LIMIT + " 次），请明日再试");
        }
        redisCache.setCacheObject(key, current + 1, 24, TimeUnit.HOURS);
    }

    /**
     * 解析导出范围：all 不限 / year 本年 / custom 自定义（校验起止）
     */
    private String[] resolveRange(String scope, String beginDate, String endDate)
    {
        if ("year".equals(scope))
        {
            String year = DateUtils.dateTimeNow("yyyy");
            return new String[] { year + "-01-01", DateUtils.dateTimeNow(DateUtils.YYYY_MM_DD) };
        }
        if ("custom".equals(scope))
        {
            if (StringUtils.isEmpty(beginDate) || StringUtils.isEmpty(endDate))
            {
                throw new ServiceException("自定义范围需要填写起止日期");
            }
            if (beginDate.compareTo(endDate) > 0)
            {
                throw new ServiceException("开始日期不能晚于结束日期");
            }
            return new String[] { beginDate, endDate };
        }
        return new String[] { null, null };
    }

    /**
     * 生成 Excel：Sheet1 打卡记录 + Sheet2 收支流水
     */
    private void writeExcel(File file, List<BizWorkLog> logs, List<BizFinanceRecord> records)
    {
        try (XSSFWorkbook wb = new XSSFWorkbook())
        {
            XSSFSheet s1 = wb.createSheet("打卡记录");
            String[] head1 = { "日期", "项目", "任务", "开始时间", "结束时间", "时长(小时)", "计费", "状态", "工作内容" };
            XSSFRow h1 = s1.createRow(0);
            for (int i = 0; i < head1.length; i++)
            {
                h1.createCell(i).setCellValue(head1[i]);
            }
            int r = 1;
            for (BizWorkLog w : logs)
            {
                XSSFRow row = s1.createRow(r++);
                int c = 0;
                row.createCell(c++).setCellValue(fmtDate(w.getLogDate()));
                row.createCell(c++).setCellValue(nvl(w.getProjectName()));
                row.createCell(c++).setCellValue(nvl(w.getTaskName()));
                row.createCell(c++).setCellValue(fmtTime(w.getStartTime()));
                row.createCell(c++).setCellValue(fmtTime(w.getEndTime()));
                row.createCell(c++).setCellValue(w.getDurationHour() != null ? w.getDurationHour().toPlainString() : "0");
                row.createCell(c++).setCellValue("0".equals(w.getBillable()) ? "计费" : "不计费");
                row.createCell(c++).setCellValue(statusText(w.getLogStatus(), LOG_STATUS));
                row.createCell(c).setCellValue(nvl(w.getContent()));
            }

            XSSFSheet s2 = wb.createSheet("收支流水");
            String[] head2 = { "日期", "类型", "类别", "项目", "金额", "税额", "结算方式", "已结算金额", "往来对象", "发票号", "交易单号", "凭证链接", "备注" };
            XSSFRow h2 = s2.createRow(0);
            for (int i = 0; i < head2.length; i++)
            {
                h2.createCell(i).setCellValue(head2[i]);
            }
            r = 1;
            for (BizFinanceRecord f : records)
            {
                XSSFRow row = s2.createRow(r++);
                int c = 0;
                row.createCell(c++).setCellValue(fmtDate(f.getRecordDate()));
                row.createCell(c++).setCellValue("0".equals(f.getRecordType()) ? "收入" : "支出");
                row.createCell(c++).setCellValue(nvl(f.getCategoryName()));
                row.createCell(c++).setCellValue(nvl(f.getProjectName()));
                row.createCell(c++).setCellValue(f.getAmount() != null ? f.getAmount().toPlainString() : "0");
                row.createCell(c++).setCellValue(f.getTaxAmount() != null ? f.getTaxAmount().toPlainString() : "0");
                row.createCell(c++).setCellValue(payMethodText(f.getPayMethod()));
                row.createCell(c++).setCellValue(f.getSettledAmount() != null ? f.getSettledAmount().toPlainString() : "0");
                row.createCell(c++).setCellValue(nvl(f.getCounterparty()));
                row.createCell(c++).setCellValue(nvl(f.getInvoiceNo()));
                row.createCell(c++).setCellValue(nvl(f.getTradeNo()));
                row.createCell(c++).setCellValue(nvl(f.getVoucherUrl()));
                row.createCell(c).setCellValue(nvl(f.getRemark()));
            }

            try (FileOutputStream fos = new FileOutputStream(file))
            {
                wb.write(fos);
            }
        }
        catch (IOException e)
        {
            throw new ServiceException("生成 Excel 失败：" + e.getMessage());
        }
    }

    /**
     * 生成 JSON 备份（PrettyFormat，含导出元信息）
     */
    private void writeJson(File file, List<BizWorkLog> logs, List<BizFinanceRecord> records,
                           String scope, String begin, String end)
    {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("app", "账时记");
        data.put("exportTime", DateUtils.dateTimeNow());
        data.put("scope", scope);
        data.put("beginDate", begin);
        data.put("endDate", end);
        data.put("workLogCount", logs.size());
        data.put("financeRecordCount", records.size());
        data.put("workLogs", logs);
        data.put("financeRecords", records);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))
        {
            w.write(JSON.toJSONString(data, JSONWriter.Feature.PrettyFormat));
        }
        catch (IOException e)
        {
            throw new ServiceException("生成 JSON 备份失败：" + e.getMessage());
        }
    }

    /**
     * 清理超过保留期的旧导出文件
     */
    private void cleanOldExports()
    {
        File dir = new File(RuoYiConfig.getProfile() + File.separator + EXPORT_DIR);
        File[] files = dir.listFiles();
        if (files == null)
        {
            return;
        }
        long expire = System.currentTimeMillis() - RETAIN_MILLIS;
        for (File f : files)
        {
            if (f.isFile() && f.lastModified() < expire
                    && (f.getName().endsWith(".xlsx") || f.getName().endsWith(".json")))
            {
                if (!f.delete())
                {
                    log.warn("[导出清理] 删除失败：{}", f.getName());
                }
            }
        }
    }

    private String payMethodText(String payMethod)
    {
        if (StringUtils.isEmpty(payMethod))
        {
            return "";
        }
        int index;
        try
        {
            index = Integer.parseInt(payMethod);
        }
        catch (NumberFormatException e)
        {
            return payMethod;
        }
        return index >= 0 && index < PAY_METHODS.length ? PAY_METHODS[index] : payMethod;
    }

    private String statusText(String status, String[] dict)
    {
        if (StringUtils.isEmpty(status))
        {
            return "";
        }
        int index;
        try
        {
            index = Integer.parseInt(status);
        }
        catch (NumberFormatException e)
        {
            return status;
        }
        return index >= 0 && index < dict.length ? dict[index] : status;
    }

    private String fmtDate(java.util.Date date)
    {
        return date == null ? "" : DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, date);
    }

    private String fmtTime(java.util.Date date)
    {
        return date == null ? "" : DateUtils.parseDateToStr("yyyy-MM-dd HH:mm", date);
    }

    private String nvl(String value)
    {
        return value == null ? "" : value;
    }
}
