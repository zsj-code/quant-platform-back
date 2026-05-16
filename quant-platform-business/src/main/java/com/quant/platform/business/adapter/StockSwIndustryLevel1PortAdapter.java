package com.quant.platform.business.adapter;

import com.quant.platform.ai.core.port.StockSwIndustryLevel1Port;
import com.quant.platform.business.stock.entity.StockIndustryValuationEntity;
import com.quant.platform.business.stock.service.StockIndustryValuationAdminService;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 从 {@code stock_industry_valuation} 取行业名，供基本面因子（如商誉 H5）按申万一级阈值匹配。
 */
@Service
public class StockSwIndustryLevel1PortAdapter implements StockSwIndustryLevel1Port {

    private final StockIndustryValuationAdminService stockIndustryValuationAdminService;

    public StockSwIndustryLevel1PortAdapter(StockIndustryValuationAdminService stockIndustryValuationAdminService) {
        this.stockIndustryValuationAdminService = stockIndustryValuationAdminService;
    }

    @Override
    public Optional<String> findSwIndustryLevel1BySecCode(String secCode) {
        if (secCode == null || secCode.isBlank()) {
            return Optional.empty();
        }
        StockIndustryValuationEntity e = stockIndustryValuationAdminService.getBySecCode(secCode.trim());
        if (e == null) {
            return Optional.empty();
        }
        String name = firstNonBlank(e.getIndustryNameFromQuote(), e.getIndustryBoardName());
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(name.trim());
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }
}
