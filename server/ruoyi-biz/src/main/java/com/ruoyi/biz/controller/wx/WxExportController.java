package com.ruoyi.biz.controller.wx;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.biz.service.IBizExportService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.config.ServerConfig;

/**
 * 小程序 · 个人数据导出（REQ-002）
 *
 * <p>生成 Excel（打卡记录 + 收支流水两个 Sheet）与 JSON 备份文件，
 * 落盘 profile/export/ 后返回下载地址；小程序端 wx.downloadFile + wx.openDocument。
 *
 * <p>留痕：@Log 自动写入操作日志（sys_oper_log），管理端"日志监控 → 操作日志"可查；
 * 限流：每用户每天最多 10 次（Redis 计数）。
 *
 * @author bookkeepingWhiz
 */
@RestController
@RequestMapping("/wx/export")
public class WxExportController extends BaseController
{
    @Autowired
    private IBizExportService exportService;

    @Autowired
    private ServerConfig serverConfig;

    /**
     * 导出个人数据
     *
     * @param body scope：all 全部 / year 本年 / custom 自定义；beginDate、endDate：自定义起止
     * @return xlsxUrl / jsonUrl / logCount / recordCount / exportTime
     */
    @Log(title = "数据导出", businessType = BusinessType.EXPORT)
    @PostMapping
    public AjaxResult export(@RequestBody(required = false) Map<String, Object> body)
    {
        String scope = body != null && body.get("scope") != null
                ? String.valueOf(body.get("scope")) : "all";
        if (!"all".equals(scope) && !"year".equals(scope) && !"custom".equals(scope))
        {
            return AjaxResult.error("导出范围不合法");
        }
        String beginDate = body != null && body.get("beginDate") != null
                ? String.valueOf(body.get("beginDate")) : null;
        String endDate = body != null && body.get("endDate") != null
                ? String.valueOf(body.get("endDate")) : null;

        Map<String, Object> result = exportService.exportUserData(getUserId(), scope, beginDate, endDate);

        // 拼接下载地址：与 RuoYi 上传文件同源（/profile/** 由 Nginx 反代到后端静态映射）
        String baseUrl = serverConfig.getUrl();
        String xlsxName = String.valueOf(result.get("xlsxFileName"));
        String jsonName = String.valueOf(result.get("jsonFileName"));
        result.put("xlsxUrl", baseUrl + Constants.RESOURCE_PREFIX + "/export/" + xlsxName);
        result.put("jsonUrl", baseUrl + Constants.RESOURCE_PREFIX + "/export/" + jsonName);

        AjaxResult ajax = AjaxResult.success("导出成功");
        ajax.put("data", result);
        return ajax;
    }
}
