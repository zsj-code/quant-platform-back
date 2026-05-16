package com.quant.platform.business.agent;

public class ResearchChatRequest {
    private String sessionId;
    private String message;
    /**
     * 可选：用于 run 锚定与因子工具的默认标的。
     */
    private String symbol;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
}

