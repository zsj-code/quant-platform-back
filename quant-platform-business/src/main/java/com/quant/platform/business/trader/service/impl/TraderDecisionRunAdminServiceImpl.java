package com.quant.platform.business.trader.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quant.platform.business.stock.entity.StockEntity;
import com.quant.platform.business.stock.mapper.StockMapper;
import com.quant.platform.business.trader.PdfResponseSupport;
import com.quant.platform.business.trader.dto.TraderDecisionRunPdfExport;
import com.quant.platform.business.trader.entity.TraderDecisionWorkflowRunEntity;
import com.quant.platform.business.trader.entity.TraderDecisionWorkflowStepEntity;
import com.quant.platform.business.trader.mapper.TraderDecisionWorkflowRunMapper;
import com.quant.platform.business.trader.mapper.TraderDecisionWorkflowStepMapper;
import com.quant.platform.business.trader.service.TraderDecisionRunAdminService;
import com.quant.platform.business.trader.vo.TraderDecisionWorkflowRunVO;
import com.quant.platform.business.trader.vo.TraderDecisionWorkflowStepVO;
import com.quant.platform.common.api.PageResult;
import com.quant.platform.common.api.ResultCode;
import com.quant.platform.common.exception.BizException;
import com.quant.platform.common.util.CommonUtil;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TraderDecisionRunAdminServiceImpl implements TraderDecisionRunAdminService {

    private static final long DEFAULT_SIZE = 20L;
    private static final long MAX_SIZE = 100L;

    private static final DateTimeFormatter PDF_FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final TraderDecisionWorkflowRunMapper traderDecisionWorkflowRunMapper;
    private final TraderDecisionWorkflowStepMapper traderDecisionWorkflowStepMapper;
    private final StockMapper stockMapper;
    private final TraderDecisionRunPdfRenderer traderDecisionRunPdfRenderer;

    public TraderDecisionRunAdminServiceImpl(TraderDecisionWorkflowRunMapper traderDecisionWorkflowRunMapper,
                                             TraderDecisionWorkflowStepMapper traderDecisionWorkflowStepMapper,
                                             StockMapper stockMapper,
                                             TraderDecisionRunPdfRenderer traderDecisionRunPdfRenderer) {
        this.traderDecisionWorkflowRunMapper = traderDecisionWorkflowRunMapper;
        this.traderDecisionWorkflowStepMapper = traderDecisionWorkflowStepMapper;
        this.stockMapper = stockMapper;
        this.traderDecisionRunPdfRenderer = traderDecisionRunPdfRenderer;
    }

    @Override
    public Optional<TraderDecisionWorkflowRunVO> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        TraderDecisionWorkflowRunEntity e = traderDecisionWorkflowRunMapper.selectById(id.trim());
        if (e == null) {
            return Optional.empty();
        }
        TraderDecisionWorkflowRunVO v = toVo(e);
        Map<String, List<TraderDecisionWorkflowStepVO>> steps = loadStepsByRunIds(List.of(e.getId()));
        v.setSteps(steps.getOrDefault(e.getId(), List.of()));
        return Optional.of(v);
    }

    @Override
    public TraderDecisionRunPdfExport buildRunPdfExport(String id) {
        if (id == null || id.isBlank()) {
            throw new BizException(ResultCode.BAD_REQUEST, "id 不能为空");
        }
        TraderDecisionWorkflowRunEntity run = traderDecisionWorkflowRunMapper.selectById(id.trim());
        if (run == null) {
            throw new BizException(ResultCode.NOT_FOUND, "运行记录不存在");
        }
        try {
            byte[] pdf = traderDecisionRunPdfRenderer.renderLlmSummaryPdf(run.getLlmSummaryText());
            String fileName = buildPdfFileName(run);
            return new TraderDecisionRunPdfExport(pdf, fileName);
        } catch (IOException ex) {
            throw new BizException(ResultCode.INTERNAL_ERROR, "生成 PDF 失败: " + ex.getMessage());
        }
    }

    private String buildPdfFileName(TraderDecisionWorkflowRunEntity run) {
        String code = CommonUtil.normalizeSixDigitCode(run.getSecCode());
        if (code == null || code.isBlank()) {
            code = sanitizeFileNamePart(run.getRequestCodeRaw(), "未知代码");
        }
        String name = resolveStockName(code);
        String time = LocalDateTime.now().format(PDF_FILE_TIME);
        String raw = code + "-" + name + "-" + time + ".pdf";
        return PdfResponseSupport.safeFilename(raw, true);
    }

    private String resolveStockName(String secCode) {
        if (secCode == null || secCode.isBlank()) {
            return "未知股票";
        }
        StockEntity stock = stockMapper.selectOne(new LambdaQueryWrapper<StockEntity>().eq(StockEntity::getCode, secCode)
            .last("LIMIT 1"));
        if (stock == null || stock.getName() == null || stock.getName().isBlank()) {
            return "未知股票";
        }
        return sanitizeFileNamePart(stock.getName(), "未知股票");
    }

    private static String sanitizeFileNamePart(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String s = raw.trim().replaceAll("[\\\\/:*?\"<>|\\r\\n]+", "_");
        return s.isEmpty() ? fallback : s;
    }

    @Override
    public PageResult<TraderDecisionWorkflowRunVO> pageByCode(String code, Long current, Long size) {
        long c = current == null || current < 1 ? 1L : current;
        long s = pageSize(size);
        String sec = CommonUtil.normalizeSixDigitCode(code);
        if (sec == null || sec.isEmpty()) {
            return PageResult.of(c, s, 0L, List.of());
        }
        var page = traderDecisionWorkflowRunMapper.selectPage(Page.of(c, s),
                new LambdaQueryWrapper<TraderDecisionWorkflowRunEntity>().eq(TraderDecisionWorkflowRunEntity::getSecCode, sec)
                        .orderByDesc(TraderDecisionWorkflowRunEntity::getStartedAt));
        List<TraderDecisionWorkflowRunEntity> runEntities = page.getRecords();
        Map<String, List<TraderDecisionWorkflowStepVO>> stepsByRunId = loadStepsByRunIds(runEntities.stream()
                .map(TraderDecisionWorkflowRunEntity::getId).collect(Collectors.toList()));
        List<TraderDecisionWorkflowRunVO> records = runEntities.stream().map(e -> {
            TraderDecisionWorkflowRunVO v = toVo(e);
            v.setSteps(stepsByRunId.getOrDefault(e.getId(), List.of()));
            return v;
        }).collect(Collectors.toList());
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    /**
     * 批量拉取步骤并按 {@code order_index} 升序挂到各 runId。
     */
    private Map<String, List<TraderDecisionWorkflowStepVO>> loadStepsByRunIds(List<String> runIds) {
        Map<String, List<TraderDecisionWorkflowStepVO>> out = new HashMap<>();
        if (runIds == null || runIds.isEmpty()) {
            return out;
        }
        List<TraderDecisionWorkflowStepEntity> rows = traderDecisionWorkflowStepMapper
                .selectList(new LambdaQueryWrapper<TraderDecisionWorkflowStepEntity>()
                        .in(TraderDecisionWorkflowStepEntity::getWorkflowRunId, runIds));
        Map<String, List<TraderDecisionWorkflowStepEntity>> grouped = rows.stream()
                .collect(Collectors.groupingBy(TraderDecisionWorkflowStepEntity::getWorkflowRunId));
        for (Map.Entry<String, List<TraderDecisionWorkflowStepEntity>> e : grouped.entrySet()) {
            List<TraderDecisionWorkflowStepEntity> list = new ArrayList<>(e.getValue());
            list.sort(Comparator.comparing(TraderDecisionWorkflowStepEntity::getOrderIndex, Comparator.nullsLast(Integer::compareTo))
                    .thenComparing(TraderDecisionWorkflowStepEntity::getStepKey, Comparator.nullsLast(String::compareTo)));
            out.put(e.getKey(), list.stream().map(TraderDecisionRunAdminServiceImpl::toStepVo).collect(Collectors.toList()));
        }
        return out;
    }

    private static TraderDecisionWorkflowStepVO toStepVo(TraderDecisionWorkflowStepEntity s) {
        if (s == null) {
            return null;
        }
        TraderDecisionWorkflowStepVO v = new TraderDecisionWorkflowStepVO();
        v.setId(s.getId());
        v.setStepKey(s.getStepKey());
        v.setStepName(s.getStepName());
        v.setOrderIndex(s.getOrderIndex());
        v.setStatus(s.getStatus());
        v.setStartedAt(s.getStartedAt());
        v.setFinishedAt(s.getFinishedAt());
        v.setDurationMillis(s.getDurationMillis());
        v.setDetail(s.getDetail());
        v.setLlmSystemPrompt(s.getLlmSystemPrompt());
        v.setLlmUserPrompt(s.getLlmUserPrompt());
        v.setLlmResponseText(s.getLlmResponseText());
        return v;
    }

    private static long pageSize(Long size) {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private static TraderDecisionWorkflowRunVO toVo(TraderDecisionWorkflowRunEntity e) {
        if (e == null) {
            return null;
        }
        TraderDecisionWorkflowRunVO v = new TraderDecisionWorkflowRunVO();
        v.setId(e.getId());
        v.setWorkflowRunKey(e.getWorkflowRunKey());
        v.setRequestCodeRaw(e.getRequestCodeRaw());
        v.setSecCode(e.getSecCode());
        v.setSourceChannel(e.getSourceChannel());
        v.setStatus(e.getStatus());
        v.setStartedAt(e.getStartedAt());
        v.setFinishedAt(e.getFinishedAt());
        v.setDecisionJson(e.getDecisionJson());
        v.setLlmSummaryText(e.getLlmSummaryText());
        v.setErrorMessage(e.getErrorMessage());
        return v;
    }
}
