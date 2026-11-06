package com.ruoyi.biz.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.biz.domain.BizFinanceRecord;
import com.ruoyi.biz.domain.BizProject;
import com.ruoyi.biz.domain.vo.CategoryStatVO;
import com.ruoyi.biz.domain.vo.ProjectProfitVO;

/**
 * 收支流水 数据层
 *
 * @author bookkeepingWhiz
 */
public interface BizFinanceRecordMapper
{
    /**
     * 查询流水列表（支持 @DataScope 部门数据权限，别名 f）
     */
    List<BizFinanceRecord> selectRecordList(BizFinanceRecord record);

    /**
     * 根据流水ID查询
     */
    BizFinanceRecord selectRecordById(Long recordId);

    /**
     * 校验流水号是否唯一
     */
    BizFinanceRecord checkRecordNoUnique(String recordNo);

    /**
     * 新增流水
     */
    int insertRecord(BizFinanceRecord record);

    /**
     * 修改流水
     */
    int updateRecord(BizFinanceRecord record);

    /**
     * 更新结算状态与已结算金额
     */
    int updateSettle(BizFinanceRecord record);

    /**
     * 逻辑删除
     */
    int deleteRecordById(@Param("recordId") Long recordId, @Param("updateBy") String updateBy);

    /**
     * 批量逻辑删除
     */
    int deleteRecordByIds(@Param("recordIds") Long[] recordIds, @Param("updateBy") String updateBy);

    /**
     * 按条件汇总金额：recordType 为空时返回净额（收入 - 支出）
     */
    BigDecimal selectTotalAmount(BizFinanceRecord record);

    /**
     * 按类别汇总
     */
    List<CategoryStatVO> selectCategoryStat(BizFinanceRecord record);

    /**
     * 项目盈亏清单（支持 @DataScope 部门数据权限，别名 p）
     */
    List<ProjectProfitVO> selectProjectProfit(BizProject project);

    /**
     * 单个项目盈亏明细
     */
    ProjectProfitVO selectProjectProfitById(Long projectId);

    /**
     * 统计项目下的流水条数（删项目前校验）
     */
    Long countRecordByProjectId(Long projectId);

    /**
     * 查询待收款（收入侧未结清）合计
     */
    BigDecimal selectReceivableTotal(BizFinanceRecord record);
}
