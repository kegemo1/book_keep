package com.ruoyi.biz.service;

import java.util.Map;

/**
 * 个人数据导出 业务层（REQ-002）
 *
 * @author bookkeepingWhiz
 */
public interface IBizExportService
{
    /**
     * 导出当前用户的工时与收支数据
     *
     * <p>生成 Excel（打卡记录 + 收支流水两个 Sheet）与 JSON 备份文件，
     * 落盘到 profile/export/ 目录；文件保留 3 天，过期由下次导出顺带清理。
     *
     * @param userId    当前微信用户ID
     * @param scope     导出范围：all 全部 / year 本年 / custom 自定义
     * @param beginDate 自定义开始日期（yyyy-MM-dd，scope=custom 时必填）
     * @param endDate   自定义结束日期（yyyy-MM-dd，scope=custom 时必填）
     * @return {logCount, recordCount, xlsxFileName, jsonFileName}
     */
    Map<String, Object> exportUserData(Long userId, String scope, String beginDate, String endDate);
}
