package com.ruoyi.biz.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.biz.domain.BizFinanceRecord;
import com.ruoyi.biz.service.IBizFinanceService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;

/**
 * 收支流水 控制层
 *
 * @author bookkeepingWhiz
 */
@RestController
@RequestMapping("/biz/finance")
public class BizFinanceRecordController extends BaseController
{
    @Autowired
    private IBizFinanceService financeService;

    @PreAuthorize("@ss.hasPermi('biz:finance:list')")
    @GetMapping("/list")
    public TableDataInfo list(BizFinanceRecord record)
    {
        startPage();
        return getDataTable(financeService.selectRecordList(record));
    }

    @PreAuthorize("@ss.hasPermi('biz:finance:query')")
    @GetMapping("/{recordId}")
    public AjaxResult getInfo(@PathVariable Long recordId)
    {
        return success(financeService.selectRecordById(recordId));
    }

    /** 按条件汇总：recordType 为空返回净额 */
    @PreAuthorize("@ss.hasPermi('biz:finance:list')")
    @GetMapping("/sum")
    public AjaxResult sum(BizFinanceRecord record)
    {
        return success(financeService.sumAmount(record));
    }

    /** 汇总待收款（收入侧未结清部分） */
    @PreAuthorize("@ss.hasPermi('biz:finance:list')")
    @GetMapping("/receivable")
    public AjaxResult receivable(BizFinanceRecord record)
    {
        return success(financeService.sumReceivable(record));
    }

    /** 类别占比统计 */
    @PreAuthorize("@ss.hasPermi('biz:finance:list')")
    @GetMapping("/stat/category")
    public AjaxResult statByCategory(BizFinanceRecord record)
    {
        return success(financeService.statByCategory(record));
    }

    @PreAuthorize("@ss.hasPermi('biz:finance:add')")
    @Log(title = "收支记账", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody BizFinanceRecord record)
    {
        return toAjax(financeService.insertRecord(record, getLoginUser()));
    }

    @PreAuthorize("@ss.hasPermi('biz:finance:edit')")
    @Log(title = "收支记账", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody BizFinanceRecord record)
    {
        return toAjax(financeService.updateRecord(record));
    }

    /** 登记结算（传本次结算金额，服务端累加） */
    @PreAuthorize("@ss.hasPermi('biz:finance:edit')")
    @Log(title = "收付款结算", businessType = BusinessType.UPDATE)
    @PutMapping("/settle")
    public AjaxResult settle(@RequestBody BizFinanceRecord record)
    {
        return toAjax(financeService.settleRecord(record));
    }

    @PreAuthorize("@ss.hasPermi('biz:finance:remove')")
    @Log(title = "收支记账", businessType = BusinessType.DELETE)
    @DeleteMapping("/{recordIds}")
    public AjaxResult remove(@PathVariable Long[] recordIds)
    {
        return toAjax(financeService.deleteRecordByIds(recordIds));
    }

    @PreAuthorize("@ss.hasPermi('biz:finance:export')")
    @Log(title = "收支记账", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BizFinanceRecord record)
    {
        List<BizFinanceRecord> list = financeService.selectRecordList(record);
        ExcelUtil<BizFinanceRecord> util = new ExcelUtil<>(BizFinanceRecord.class);
        util.exportExcel(response, list, "收支流水");
    }
}
