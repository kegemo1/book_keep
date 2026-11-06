package com.ruoyi.biz.service;

import java.util.List;
import com.ruoyi.biz.domain.BizFinanceRecord;
import com.ruoyi.biz.domain.BizProject;
import com.ruoyi.biz.domain.vo.CategoryStatVO;
import com.ruoyi.biz.domain.vo.DashboardVO;
import com.ruoyi.biz.domain.vo.ProjectProfitVO;

/**
 * 统计报表 业务层
 *
 * @author bookkeepingWhiz
 */
public interface IBizStatService
{
    /**
     * 工作台 / 小程序首页概览
     *
     * @param userId 当前用户ID
     * @param deptId 当前用户部门ID（用于项目计数）
     */
    DashboardVO dashboard(Long userId, Long deptId);

    /**
     * 项目盈亏清单（毛利率 / 人力成本 / 净利 / 预算使用率）
     */
    List<ProjectProfitVO> selectProjectProfit(BizProject project);

    /**
     * 单项目盈亏明细
     */
    ProjectProfitVO selectProjectProfitDetail(Long projectId);

    /**
     * 收支类别占比
     */
    List<CategoryStatVO> selectCategoryStat(BizFinanceRecord record);
}
