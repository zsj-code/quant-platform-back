package com.quant.platform.business.agent;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResearchStopResponse {
    private boolean stopped;
    private String sessionId;
    private String runId;
}
