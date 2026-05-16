package com.quant.platform.ai.core.port;

import com.quant.platform.common.dto.StockValuationSnapshotDTO;

public interface StockValuationSnapshotPort {

    StockValuationSnapshotDTO findLatestBySymbol(String symbol);
}

