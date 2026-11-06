package com.ruoyi.biz.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.biz.constant.BizConstants;
import com.ruoyi.biz.domain.BizFinanceCategory;
import com.ruoyi.biz.domain.BizFinanceRecord;
import com.ruoyi.biz.domain.vo.CategoryStatVO;
import com.ruoyi.biz.mapper.BizFinanceCategoryMapper;
import com.ruoyi.biz.mapper.BizFinanceRecordMapper;
import com.ruoyi.biz.mapper.BizProjectMapper;
import com.ruoyi.biz.service.IBizFinanceService;
import com.ruoyi.biz.util.BizDateUtils;
import com.ruoyi.biz.util.BizMathUtils;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;

/**
 * 收支记账 业务层实现
 *
 * <p>记两条铁律：
 * <ol>
 *   <li>金额恒为正数，收/支方向由 recordType 决定，避免正负号混用导致统计口径分裂</li>
 *   <li>类别方向必须与流水方向一致，否则报表会同时出现在收入与支出两侧</li>
 * </ol>
 *
 * @author bookkeepingWhiz
 */
@Service
public class BizFinanceServiceImpl implements IBizFinanceService
{
    /** 流水号随机后缀尝试次数 */
    private static final int RECORD_NO_RETRY = 10;

    @Autowired
    private BizFinanceCategoryMapper categoryMapper;

    @Autowired
    private BizFinanceRecordMapper recordMapper;

    @Autowired
    private BizProjectMapper projectMapper;

    // ------------------------------------------------------------------
    // 收支类别
    // ------------------------------------------------------------------

    @Override
    public List<BizFinanceCategory> selectCategoryList(BizFinanceCategory category)
    {
        return categoryMapper.selectCategoryList(category);
    }

    @Override
    public List<BizFinanceCategory> selectCategoryOptions(String categoryType)
    {
        return categoryMapper.selectCategoryOptions(categoryType);
    }

    @Override
    public List<BizFinanceCategory> buildCategoryTree(List<BizFinanceCategory> list)
    {
        List<BizFinanceCategory> roots = new ArrayList<>();
        if (list == null || list.isEmpty())
        {
            return roots;
        }
        Map<Long, BizFinanceCategory> dict = new HashMap<>(list.size());
        for (BizFinanceCategory node : list)
        {
            dict.put(node.getCategoryId(), node);
        }
        for (BizFinanceCategory node : list)
        {
            Long parentId = node.getParentId();
            BizFinanceCategory parent = parentId == null ? null : dict.get(parentId);
            if (parent != null)
            {
                parent.getChildren().add(node);
            }
            else
            {
                roots.add(node);
            }
        }
        return roots;
    }

    @Override
    public BizFinanceCategory selectCategoryById(Long categoryId)
    {
        return categoryMapper.selectCategoryById(categoryId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertCategory(BizFinanceCategory category)
    {
        if (!checkCategoryNameUnique(category))
        {
            throw new ServiceException("同类目下已存在类别「" + category.getCategoryName() + "」");
        }
        fillCategoryDefaults(category, true);
        return categoryMapper.insertCategory(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateCategory(BizFinanceCategory category)
    {
        BizFinanceCategory db = categoryMapper.selectCategoryById(category.getCategoryId());
        if (db == null)
        {
            throw new ServiceException("收支类别不存在或已删除");
        }
        if (StringUtils.isNotEmpty(category.getCategoryName()) && !category.getCategoryName().equals(db.getCategoryName())
                && !checkCategoryNameUnique(category))
        {
            throw new ServiceException("同类目下已存在类别「" + category.getCategoryName() + "」");
        }
        category.setUpdateBy(SecurityUtils.getUsername());
        category.setUpdateTime(DateUtils.getNowDate());
        return categoryMapper.updateCategory(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteCategoryById(Long categoryId)
    {
        BizFinanceCategory db = categoryMapper.selectCategoryById(categoryId);
        if (db == null)
        {
            throw new ServiceException("收支类别不存在或已删除");
        }
        Long sysDefault = categoryMapper.countSysDefault(categoryId);
        if (sysDefault != null && sysDefault > 0)
        {
            throw new ServiceException("「" + db.getCategoryName() + "」为系统预置类别，不允许删除（可改为停用）");
        }
        Long children = categoryMapper.countChildrenByParentId(categoryId);
        if (children != null && children > 0)
        {
            throw new ServiceException("该类别下还有 " + children + " 个子类别，请先处理子类别");
        }
        Long used = categoryMapper.countRecordByCategoryId(categoryId);
        if (used != null && used > 0)
        {
            throw new ServiceException("该类别已被 " + used + " 条流水引用，不允许删除（可改为停用）");
        }
        return categoryMapper.deleteCategoryById(categoryId, SecurityUtils.getUsername());
    }

    @Override
    public boolean checkCategoryNameUnique(BizFinanceCategory category)
    {
        Long parentId = category.getParentId() == null ? 0L : category.getParentId();
        BizFinanceCategory exist = categoryMapper.checkCategoryNameUnique(category.getCategoryType(), parentId,
                category.getCategoryName());
        if (exist == null)
        {
            return true;
        }
        return exist.getCategoryId().equals(category.getCategoryId());
    }

    // ------------------------------------------------------------------
    // 收支流水
    // ------------------------------------------------------------------

    @Override
    @DataScope(deptAlias = "f", userAlias = "f")
    public List<BizFinanceRecord> selectRecordList(BizFinanceRecord record)
    {
        return recordMapper.selectRecordList(record);
    }

    @Override
    public BizFinanceRecord selectRecordById(Long recordId)
    {
        return recordMapper.selectRecordById(recordId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertRecord(BizFinanceRecord record, LoginUser loginUser)
    {
        validateRecord(record);
        Date now = DateUtils.getNowDate();
        record.setRecordNo(generateRecordNo());
        record.setUserId(loginUser.getUserId());
        record.setDeptId(loginUser.getDeptId());
        if (StringUtils.isEmpty(record.getCurrency()))
        {
            record.setCurrency("CNY");
        }
        if (record.getTaxAmount() == null)
        {
            record.setTaxAmount(BigDecimal.ZERO);
        }
        if (record.getSettledAmount() == null)
        {
            record.setSettledAmount(BigDecimal.ZERO);
        }
        record.setSettleStatus(calcSettleStatus(record.getAmount(), record.getSettledAmount()));
        if (StringUtils.isEmpty(record.getSource()))
        {
            record.setSource(BizConstants.Source.PC);
        }
        record.setDelFlag(BizConstants.DEL_FLAG_NORMAL);
        record.setCreateBy(loginUser.getUsername());
        record.setCreateTime(now);
        try
        {
            return recordMapper.insertRecord(record);
        }
        catch (DuplicateKeyException e)
        {
            // 极低概率的流水号碰撞：换号重试一次
            record.setRecordNo(generateRecordNo());
            return recordMapper.insertRecord(record);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateRecord(BizFinanceRecord record)
    {
        BizFinanceRecord db = recordMapper.selectRecordById(record.getRecordId());
        if (db == null)
        {
            throw new ServiceException("流水不存在或已删除");
        }
        if (!SecurityUtils.getUserId().equals(db.getUserId()) && !SecurityUtils.isAdmin())
        {
            throw new ServiceException("只能修改自己登记的流水");
        }
        // 类别方向一致性：以更新后的最终值判断
        String finalType = StringUtils.isNotEmpty(record.getRecordType()) ? record.getRecordType()
                : db.getRecordType();
        Long finalCategoryId = record.getCategoryId() != null ? record.getCategoryId() : db.getCategoryId();
        checkCategoryMatch(finalCategoryId, finalType);

        BigDecimal finalAmount = record.getAmount() != null ? record.getAmount() : db.getAmount();
        BigDecimal finalSettled = record.getSettledAmount() != null ? record.getSettledAmount()
                : db.getSettledAmount();
        if (record.getAmount() != null && !validAmount(record.getAmount()))
        {
            throw new ServiceException("金额必须大于 0 且不超过 " + BizConstants.MAX_AMOUNT + " 元");
        }
        if (BizMathUtils.nvl(finalSettled).compareTo(BizMathUtils.nvl(finalAmount)) > 0)
        {
            throw new ServiceException("已结算金额不能超过流水金额");
        }
        record.setSettleStatus(calcSettleStatus(finalAmount, finalSettled));
        record.setUpdateBy(SecurityUtils.getUsername());
        record.setUpdateTime(DateUtils.getNowDate());
        return recordMapper.updateRecord(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int settleRecord(BizFinanceRecord record)
    {
        BizFinanceRecord db = recordMapper.selectRecordById(record.getRecordId());
        if (db == null)
        {
            throw new ServiceException("流水不存在或已删除");
        }
        // 约定：settledAmount 传「本次结算金额」，服务端累加
        BigDecimal thisTime = BizMathUtils.nvl(record.getSettledAmount());
        if (thisTime.signum() <= 0)
        {
            throw new ServiceException("本次结算金额必须大于 0");
        }
        BigDecimal total = BizMathUtils.add(db.getSettledAmount(), thisTime);
        if (total.compareTo(BizMathUtils.nvl(db.getAmount())) > 0)
        {
            throw new ServiceException("累计结算金额将超过流水金额，请核对");
        }
        BizFinanceRecord update = new BizFinanceRecord();
        update.setRecordId(db.getRecordId());
        update.setSettledAmount(total);
        update.setSettleStatus(calcSettleStatus(db.getAmount(), total));
        update.setUpdateBy(SecurityUtils.getUsername());
        update.setUpdateTime(DateUtils.getNowDate());
        return recordMapper.updateSettle(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRecordById(Long recordId)
    {
        BizFinanceRecord db = recordMapper.selectRecordById(recordId);
        if (db == null)
        {
            throw new ServiceException("流水不存在或已删除");
        }
        if (!SecurityUtils.getUserId().equals(db.getUserId()) && !SecurityUtils.isAdmin())
        {
            throw new ServiceException("只能删除自己登记的流水");
        }
        return recordMapper.deleteRecordById(recordId, SecurityUtils.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRecordByIds(Long[] recordIds)
    {
        if (recordIds == null || recordIds.length == 0)
        {
            return 0;
        }
        for (Long recordId : recordIds)
        {
            BizFinanceRecord db = recordMapper.selectRecordById(recordId);
            if (db == null)
            {
                throw new ServiceException("流水 #" + recordId + " 不存在或已删除");
            }
            if (!SecurityUtils.getUserId().equals(db.getUserId()) && !SecurityUtils.isAdmin())
            {
                throw new ServiceException("存在非本人登记的流水，无法批量删除");
            }
        }
        return recordMapper.deleteRecordByIds(recordIds, SecurityUtils.getUsername());
    }

    @Override
    @DataScope(deptAlias = "f", userAlias = "f")
    public List<CategoryStatVO> statByCategory(BizFinanceRecord record)
    {
        List<CategoryStatVO> list = recordMapper.selectCategoryStat(record);
        if (list == null || list.isEmpty())
        {
            return list;
        }
        // 按收入/支出分别计算占比
        Map<String, BigDecimal> totals = new HashMap<>();
        for (CategoryStatVO vo : list)
        {
            String type = vo.getCategoryType() == null ? "0" : vo.getCategoryType();
            totals.put(type, BizMathUtils.add(totals.get(type), vo.getTotalAmount()));
        }
        for (CategoryStatVO vo : list)
        {
            String type = vo.getCategoryType() == null ? "0" : vo.getCategoryType();
            vo.setTotalAmount(BizMathUtils.nvl(vo.getTotalAmount()));
            vo.setRatio(BizMathUtils.percent(vo.getTotalAmount(), totals.get(type)));
        }
        return list;
    }

    @Override
    public BigDecimal sumAmount(BizFinanceRecord record)
    {
        BigDecimal total = recordMapper.selectTotalAmount(record);
        return BizMathUtils.nvl(total);
    }

    @Override
    public BigDecimal sumReceivable(BizFinanceRecord record)
    {
        BigDecimal total = recordMapper.selectReceivableTotal(record);
        return BizMathUtils.nvl(total);
    }

    @Override
    public String generateRecordNo()
    {
        String dayPart = DateUtils.parseDateToStr("yyyyMMdd", DateUtils.getNowDate());
        String prefix = BizConstants.RECORD_NO_PREFIX + dayPart;
        for (int i = 0; i < RECORD_NO_RETRY; i++)
        {
            int random = (int) (Math.random() * 10000);
            String candidate = prefix + String.format("%04d", random);
            if (recordMapper.checkRecordNoUnique(candidate) == null)
            {
                return candidate;
            }
        }
        // 兜底：用毫秒后缀，保证唯一性
        return prefix + String.valueOf(System.currentTimeMillis()).substring(7);
    }

    // ------------------------------------------------------------------
    // 私有方法
    // ------------------------------------------------------------------

    private void validateRecord(BizFinanceRecord record)
    {
        if (!BizConstants.RecordType.INCOME.equals(record.getRecordType())
                && !BizConstants.RecordType.EXPENSE.equals(record.getRecordType()))
        {
            throw new ServiceException("收支类型不合法");
        }
        if (!validAmount(record.getAmount()))
        {
            throw new ServiceException("金额必须大于 0 且不超过 " + BizConstants.MAX_AMOUNT + " 元");
        }
        if (record.getRecordDate() == null)
        {
            record.setRecordDate(DateUtils.getNowDate());
        }
        if (BizDateUtils.isFuture(record.getRecordDate()))
        {
            throw new ServiceException("发生日期不能晚于今天");
        }
        if (record.getProjectId() != null && projectMapper.selectProjectById(record.getProjectId()) == null)
        {
            throw new ServiceException("项目不存在或已删除");
        }
        if (record.getTaskId() != null && record.getProjectId() == null)
        {
            throw new ServiceException("指定任务时必须同时指定所属项目");
        }
        if (!BizMathUtils.nvl(record.getSettledAmount()).equals(BigDecimal.ZERO)
                && BizMathUtils.nvl(record.getSettledAmount()).compareTo(BizMathUtils.nvl(record.getAmount())) > 0)
        {
            throw new ServiceException("已结算金额不能超过流水金额");
        }
        checkCategoryMatch(record.getCategoryId(), record.getRecordType());
    }

    /**
     * 类别方向必须与流水方向一致
     */
    private void checkCategoryMatch(Long categoryId, String recordType)
    {
        if (categoryId == null)
        {
            return;
        }
        BizFinanceCategory category = categoryMapper.selectCategoryById(categoryId);
        if (category == null)
        {
            throw new ServiceException("收支类别不存在或已删除");
        }
        if (!category.getCategoryType().equals(recordType))
        {
            throw new ServiceException("类别「" + category.getCategoryName() + "」属于"
                    + (BizConstants.RecordType.INCOME.equals(category.getCategoryType()) ? "收入" : "支出")
                    + "类目，与当前记账方向不一致");
        }
    }

    private boolean validAmount(BigDecimal amount)
    {
        if (amount == null || amount.signum() <= 0)
        {
            return false;
        }
        return amount.compareTo(BizConstants.MAX_AMOUNT) <= 0;
    }

    /**
     * 根据金额与已结算金额推导结算状态
     */
    private String calcSettleStatus(BigDecimal amount, BigDecimal settledAmount)
    {
        BigDecimal amountValue = BizMathUtils.nvl(amount);
        BigDecimal settledValue = BizMathUtils.nvl(settledAmount);
        if (settledValue.signum() <= 0)
        {
            return BizConstants.SettleStatus.UNSETTLED;
        }
        if (settledValue.compareTo(amountValue) >= 0)
        {
            return BizConstants.SettleStatus.SETTLED;
        }
        return BizConstants.SettleStatus.PARTIAL;
    }

    private void fillCategoryDefaults(BizFinanceCategory category, boolean isInsert)
    {
        Date now = DateUtils.getNowDate();
        if (category.getParentId() == null)
        {
            category.setParentId(0L);
        }
        // 维护祖级列表，便于按父类汇总
        if (category.getParentId() != 0L)
        {
            BizFinanceCategory parent = categoryMapper.selectCategoryById(category.getParentId());
            if (parent == null)
            {
                throw new ServiceException("父类别不存在或已删除");
            }
            if (!parent.getCategoryType().equals(category.getCategoryType()))
            {
                throw new ServiceException("子类别必须与父类别的收支方向一致");
            }
            category.setAncestors(StringUtils.isEmpty(parent.getAncestors()) ? String.valueOf(parent.getCategoryId())
                    : parent.getAncestors() + "," + parent.getCategoryId());
        }
        else
        {
            category.setAncestors("0");
        }
        if (StringUtils.isEmpty(category.getStatus()))
        {
            category.setStatus(BizConstants.STATUS_NORMAL);
        }
        if (StringUtils.isEmpty(category.getSysDefault()))
        {
            category.setSysDefault("N");
        }
        if (category.getOrderNum() == null)
        {
            category.setOrderNum(0);
        }
        category.setDelFlag(BizConstants.DEL_FLAG_NORMAL);
        if (isInsert)
        {
            category.setCreateBy(SecurityUtils.getUsername());
            category.setCreateTime(now);
        }
        else
        {
            category.setUpdateBy(SecurityUtils.getUsername());
            category.setUpdateTime(now);
        }
    }
}
