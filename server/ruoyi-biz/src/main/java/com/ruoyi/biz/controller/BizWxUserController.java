package com.ruoyi.biz.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.biz.constant.BizConstants;
import com.ruoyi.biz.domain.BizWxUser;
import com.ruoyi.biz.service.IBizWxUserService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;

/**
 * 微信用户 控制层（管理端）
 *
 * <p>只读/管理 biz_wx_user 档案，与 sys_user 完全隔离；不提供任何绑定能力。
 *
 * @author bookkeepingWhiz
 */
@RestController
@RequestMapping("/biz/wxuser")
public class BizWxUserController extends BaseController
{
    @Autowired
    private IBizWxUserService wxUserService;

    @PreAuthorize("@ss.hasPermi('biz:wxuser:list')")
    @GetMapping("/list")
    public TableDataInfo list(BizWxUser wxUser)
    {
        startPage();
        List<BizWxUser> list = wxUserService.selectWxUserList(wxUser);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('biz:wxuser:query')")
    @GetMapping("/{wxUserId}")
    public AjaxResult getInfo(@PathVariable Long wxUserId)
    {
        return success(wxUserService.selectWxUserById(wxUserId));
    }

    /**
     * 修改微信用户资料 / 停用启用
     */
    @PreAuthorize("@ss.hasPermi('biz:wxuser:edit')")
    @Log(title = "微信用户", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody BizWxUser wxUser)
    {
        return toAjax(wxUserService.updateWxUser(wxUser));
    }

    /**
     * 停用 / 启用：停用后该微信无法登录小程序
     */
    @PreAuthorize("@ss.hasPermi('biz:wxuser:edit')")
    @Log(title = "微信用户", businessType = BusinessType.UPDATE)
    @PutMapping("/status/{wxUserId}")
    public AjaxResult changeStatus(@PathVariable Long wxUserId, String status)
    {
        BizWxUser update = new BizWxUser();
        update.setWxUserId(wxUserId);
        update.setStatus(BizConstants.STATUS_NORMAL.equals(status) ? BizConstants.STATUS_NORMAL : "1");
        return toAjax(wxUserService.updateWxUser(update));
    }

    @PreAuthorize("@ss.hasPermi('biz:wxuser:remove')")
    @Log(title = "微信用户", businessType = BusinessType.DELETE)
    @DeleteMapping("/{wxUserId}")
    public AjaxResult remove(@PathVariable Long wxUserId)
    {
        return toAjax(wxUserService.deleteWxUserById(wxUserId));
    }
}
