package com.quant.platform.business.stock.controller;


import com.quant.platform.business.stock.convert.StockAnnouncementAdminConvert;
import com.quant.platform.business.stock.service.StockAnnouncementAdminService;
import com.quant.platform.business.stock.vo.StockAnnouncementVO;
import com.quant.platform.common.api.PageResult;
import com.quant.platform.common.api.Result;
import com.quant.platform.common.api.ResultCode;
import com.quant.platform.common.exception.BizException;
import com.quant.platform.common.util.CommonUtil;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/fundamental/stock-announcements")
public class StockAnnouncementController {

    private final StockAnnouncementAdminService stockAnnouncementAdminService;

    public StockAnnouncementController(StockAnnouncementAdminService stockAnnouncementAdminService) {
        this.stockAnnouncementAdminService = stockAnnouncementAdminService;
    }

    @GetMapping("/detail/{id}")
    public Result<StockAnnouncementVO> getById(
            @PathVariable(name = "id") @NotBlank(message = "id 不能为空") String id) {
        var entity = stockAnnouncementAdminService.getById(id);
        return Result.ok(StockAnnouncementAdminConvert.INSTANCE.toVO(entity));
    }

    /**
     * 按股票编码分页查询公告。
     */
    @GetMapping("/page")
    public Result<PageResult<StockAnnouncementVO>> pageByCode(
            @RequestParam(name = "code") @NotBlank(message = "code 不能为空") String code,
            @RequestParam(name = "current", defaultValue = "1") @Min(value = 1, message = "current 不能小于 1") Long current,
            @RequestParam(name = "size", defaultValue = "20") @Min(value = 1, message = "size 不能小于 1") @Max(value = 500,
                    message = "size 过大") Long size) {
        String c = CommonUtil.normalizeSixDigitCode(code);
        if (c == null || c.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "股票编码无效");
        }
        var p = stockAnnouncementAdminService.pageByCode(c, current, size);
        return Result.ok(PageResult.of(p.getCurrent(), p.getSize(), p.getTotal(),
                StockAnnouncementAdminConvert.INSTANCE.toVOList(p.getRecords())));
    }
}
