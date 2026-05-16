package com.quant.platform.business.agent;

public class ResearchChatResponse {
    private String runId;
    private String workflowRunKey;
    private String answer;

    public ResearchChatResponse() {
    }

    public ResearchChatResponse(String runId, String workflowRunKey, String answer) {
        this.runId = runId;
        this.workflowRunKey = workflowRunKey;
        this.answer = answer;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getWorkflowRunKey() {
        return workflowRunKey;
    }

    public void setWorkflowRunKey(String workflowRunKey) {
        this.workflowRunKey = workflowRunKey;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}

