package com.quant.platform.business.kline.controller;

import com.quant.platform.business.kline.convert.KlineBarAdminConvert;
import com.quant.platform.business.kline.service.KlineBarAdminService;
import com.quant.platform.business.kline.vo.KlineBarVO;
import com.quant.platform.common.api.PageResult;
import com.quant.platform.common.api.Result;
import com.quant.platform.common.util.CommonUtil;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/kline-bars")
@Validated
public class KlineBarController {
    private final KlineBarAdminService klineBarAdminService;

    public KlineBarController(KlineBarAdminService klineBarAdminService) {
        this.klineBarAdminService = klineBarAdminService;
    }

    @GetMapping("/page")
    public Result<PageResult<KlineBarVO>> page(
            @RequestParam(name = "code") @NotBlank(message = "code 不能为空") String code,
            @RequestParam(name = "intervalType") @NotBlank(message = "intervalType 不能为空") String intervalType,
            @RequestParam(name = "startTime", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss", fallbackPatterns = {
                    "yyyy-MM-dd'T'HH:mm:ss"}) LocalDateTime startTime,
            @RequestParam(name = "endTime", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss", fallbackPatterns = {
                    "yyyy-MM-dd'T'HH:mm:ss"}) LocalDateTime endTime,
            @RequestParam(name = "current", defaultValue = "1") @Min(value = 1, message = "current 不能小于 1") Long current,
            @RequestParam(name = "size", defaultValue = "500") @Min(value = 1, message = "size 不能小于 1") @Max(value = 2000, message = "size 过大（最大 2000）") Long size) {
        LocalDateTime s = startTime != null ? startTime : LocalDateTime.of(2026, 1, 1, 0, 0, 0);
        LocalDateTime e = endTime != null ? endTime : LocalDate.now().atTime(LocalTime.MAX);
        String symbol = CommonUtil.toSymbol(code);
        var p = klineBarAdminService.page(symbol, intervalType, s, e, current, size);
        return Result.ok(PageResult.of(p.getCurrent(), p.getSize(), p.getTotal(),
                KlineBarAdminConvert.INSTANCE.toVOList(p.getRecords())));
    }

    @GetMapping("/list")
    public Result<List<KlineBarVO>> list(@RequestParam(name = "code") @NotBlank(message = "code 不能为空") String code,
            @RequestParam(name = "intervalType") @NotBlank(message = "intervalType 不能为空") String intervalType,
            @RequestParam(name = "startTime", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss", fallbackPatterns = {
                    "yyyy-MM-dd'T'HH:mm:ss"}) LocalDateTime startTime,
            @RequestParam(name = "endTime", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss", fallbackPatterns = {
                    "yyyy-MM-dd'T'HH:mm:ss"}) LocalDateTime endTime) {
        LocalDateTime s = startTime != null ? startTime : LocalDateTime.of(2026, 1, 1, 0, 0, 0);
        LocalDateTime e = endTime != null ? endTime : LocalDate.now().atTime(LocalTime.MAX);
        String symbol = CommonUtil.toSymbol(code);
        return Result.ok(KlineBarAdminConvert.INSTANCE.toVOList(klineBarAdminService.list(symbol, intervalType, s, e)));
    }
}
