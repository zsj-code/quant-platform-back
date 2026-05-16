package com.quant.platform.business.trader.controller;

import com.quant.platform.business.trader.PdfResponseSupport;
import com.quant.platform.business.trader.service.TraderDecisionRunAdminService;
import com.quant.platform.business.trader.vo.TraderDecisionWorkflowRunVO;
import com.quant.platform.common.api.PageResult;
import com.quant.platform.common.api.Result;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 交易决策运行记录：分页与详情；PDF 预览/导出仅基于 {@code trader_decision_workflow_run.llm_summary_text}。
 */
@Validated
@RestController
@RequestMapping("/api/trader-decisions")
public class TraderDecisionRunController {

    private final TraderDecisionRunAdminService traderDecisionRunAdminService;

    public TraderDecisionRunController(TraderDecisionRunAdminService traderDecisionRunAdminService) {
        this.traderDecisionRunAdminService = traderDecisionRunAdminService;
    }

    /**
     * @param code
     *            股票编码（6 位或可规范化写法）
     */
    @GetMapping("/page")
    public Result<PageResult<TraderDecisionWorkflowRunVO>> page(
            @RequestParam(name = "code") @NotBlank(message = "code 不能为空") String code,
            @RequestParam(name = "current", defaultValue = "1") @Min(value = 1, message = "current 不能小于 1") Long current,
            @RequestParam(name = "size", defaultValue = "20") @Min(value = 1, message = "size 不能小于 1") @Max(value = 100,
                    message = "size 过大（最大 100）") Long size) {
        return Result.ok(traderDecisionRunAdminService.pageByCode(code, current, size));
    }

    /**
     * PDF 预览（{@code Content-Disposition: inline}）；正文仅为 {@code llm_summary_text}，不含各步骤。
     */
    @GetMapping("/{id}/pdf/preview")
    public ResponseEntity<byte[]> previewPdf(@PathVariable("id") @NotBlank(message = "id 不能为空") String id) {
        return PdfResponseSupport.previewEntity(null, pdfFileName(id));
    }

    /**
     * PDF 导出（{@code Content-Disposition: attachment}）；正文仅为 {@code llm_summary_text}，不含各步骤。
     */
    @GetMapping("/{id}/pdf/export")
    public ResponseEntity<byte[]> exportPdf(@PathVariable("id") @NotBlank(message = "id 不能为空") String id) {
        byte[] pdf = null;
        return PdfResponseSupport.downloadEntity(pdf, pdfFileName(id));
    }

    private static String pdfFileName(String runId) {
        return "trader-decision-" + runId + ".pdf";
    }
}
