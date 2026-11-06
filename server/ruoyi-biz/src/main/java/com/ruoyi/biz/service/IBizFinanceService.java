package com.ruoyi.biz.service;

import java.math.BigDecimal;
import java.util.List;
import com.ruoyi.biz.domain.BizFinanceCategory;
import com.ruoyi.biz.domain.BizFinanceRecord;
import com.ruoyi.biz.domain.vo.CategoryStatVO;
import com.ruoyi.common.core.domain.model.LoginUser;

/**
 * 收支记账 业务层（类别 + 流水）
 *
 * @author bookkeepingWhiz
 */
public interface IBizFinanceService
{
    // ---------------- 收支类别 ----------------

    /**
     * 查询类别列表
     */
    List<BizFinanceCategory> selectCategoryList(BizFinanceCategory category);

    /**
     * 查询类别下拉选项（只返回启用的）
     */
    List<BizFinanceCategory> selectCategoryOptions(String categoryType);

    /**
     * 类别列表转树
     */
    List<BizFinanceCategory> buildCategoryTree(List<BizFinanceCategory> list);

    /**
     * 根据类别ID查询
     */
    BizFinanceCategory selectCategoryById(Long categoryId);

    /**
     * 新增类别
     */
    int insertCategory(BizFinanceCategory category);

    /**
     * 修改类别
     */
    int updateCategory(BizFinanceCategory category);

    /**
     * 删除类别（系统预置、存在子类别、已被流水引用时禁止删除）
     */
    int deleteCategoryById(Long categoryId);

    /**
     * 校验同方向同父级下类别名唯一
     */
    boolean checkCategoryNameUnique(BizFinanceCategory category);

    // ---------------- 收支流水 ----------------

    /**
     * 查询流水列表
     */
    List<BizFinanceRecord> selectRecordList(BizFinanceRecord record);

    /**
     * 根据流水ID查询
     */
    BizFinanceRecord selectRecordById(Long recordId);

    /**
     * 新增流水：自动生成流水号、校验金额与项目/类别归属
     */
    int insertRecord(BizFinanceRecord record, LoginUser loginUser);

    /**
     * 修改流水
     */
    int updateRecord(BizFinanceRecord record);

    /**
     * 登记结算（部分收款/付款）
     */
    int settleRecord(BizFinanceRecord record);

    /**
     * 逻辑删除流水
     */
    int deleteRecordById(Long recordId);

    /**
     * 批量逻辑删除流水
     */
    int deleteRecordByIds(Long[] recordIds);

    /**
     * 按类别汇总金额
     */
    List<CategoryStatVO> statByCategory(BizFinanceRecord record);

    /**
     * 按条件汇总金额：recordType 为空时返回净额（收入 - 支出）
     */
    BigDecimal sumAmount(BizFinanceRecord record);

    /**
     * 汇总待收款（收入侧未结清部分）
     */
    BigDecimal sumReceivable(BizFinanceRecord record);

    /**
     * 生成业务流水号（BK + yyyyMMdd + 4 位序号），保证唯一
     */
    String generateRecordNo();
}
