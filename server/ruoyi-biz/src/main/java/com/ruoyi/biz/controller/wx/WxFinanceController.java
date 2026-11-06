package com.ruoyi.biz.controller.wx;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.biz.constant.BizConstants;
import com.ruoyi.biz.domain.BizFinanceRecord;
import com.ruoyi.biz.service.IBizFinanceService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.biz.util.BizDateUtils;

/**
 * 小程序 · 收支记账
 *
 * @author bookkeepingWhiz
 */
@RestController
@RequestMapping("/wx/finance")
public class WxFinanceController extends BaseController
{
    @Autowired
    private IBizFinanceService financeService;

    /** 类别下拉（0收入 1支出） */
    @GetMapping("/categories")
    public AjaxResult categories(String categoryType)
    {
        return AjaxResult.success(financeService.selectCategoryOptions(categoryType));
    }

    /**
     * 按类别统计金额（图表用，REQ-003 环形图）
     * @param recordType 0=收入 1=支出
     */
    @GetMapping("/stat/category")
    public AjaxResult statByCategory(@RequestParam(defaultValue = "1") String recordType)
    {
        BizFinanceRecord query = new BizFinanceRecord();
        query.setUserId(getUserId());
        query.setRecordType(recordType);
        return AjaxResult.success(financeService.statByCategory(query));
    }

    /** 记一笔 */
    @PostMapping("/add")
    public AjaxResult add(@RequestBody BizFinanceRecord record)
    {
        record.setSource(BizConstants.Source.MINIAPP);
        financeService.insertRecord(record, getLoginUser());
        return AjaxResult.success("记账成功", record);
    }

    /** 我的账单 */
    @GetMapping("/list")
    public TableDataInfo list(BizFinanceRecord record)
    {
        record.setUserId(getUserId());
        startPage();
        return getDataTable(financeService.selectRecordList(record));
    }

    /** 本月收支汇总 */
    @GetMapping("/summary")
    public AjaxResult summary()
    {
        String today = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.getNowDate());
        String monthBegin = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, BizDateUtils.monthStart());

        BizFinanceRecord income = new BizFinanceRecord();
        income.setUserId(getUserId());
        income.setRecordType(BizConstants.RecordType.INCOME);
        income.getParams().put("beginRecordDate", monthBegin);
        income.getParams().put("endRecordDate", today);

        BizFinanceRecord expense = new BizFinanceRecord();
        expense.setUserId(getUserId());
        expense.setRecordType(BizConstants.RecordType.EXPENSE);
        expense.getParams().put("beginRecordDate", monthBegin);
        expense.getParams().put("endRecordDate", today);

        BizFinanceRecord receivable = new BizFinanceRecord();
        receivable.setUserId(getUserId());

        AjaxResult ajax = AjaxResult.success();
        ajax.put("monthIncome", financeService.sumAmount(income));
        ajax.put("monthExpense", financeService.sumAmount(expense));
        ajax.put("receivable", financeService.sumReceivable(receivable));
        ajax.put("categoryStat", financeService.statByCategory(income));
        return ajax;
    }

    /** 登记收付款（传本次结算金额） */
    @PostMapping("/settle")
    public AjaxResult settle(@RequestBody BizFinanceRecord record)
    {
        return toAjax(financeService.settleRecord(record));
    }
}
