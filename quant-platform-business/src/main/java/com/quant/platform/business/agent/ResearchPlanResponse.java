package com.quant.platform.business.agent;


import com.quant.platform.ai.core.langchain4j.research.plan.ResearchPlan;

public class ResearchPlanResponse {
    private boolean ok;
    private String error;
    private ResearchPlan plan;
    private String rawModelJson;

    public static ResearchPlanResponse ok(ResearchPlan plan, String rawModelJson) {
        ResearchPlanResponse r = new ResearchPlanResponse();
        r.ok = true;
        r.plan = plan;
        r.rawModelJson = rawModelJson;
        return r;
    }

    public static ResearchPlanResponse fail(String error, String rawModelJson) {
        ResearchPlanResponse r = new ResearchPlanResponse();
        r.ok = false;
        r.error = error;
        r.rawModelJson = rawModelJson;
        return r;
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public ResearchPlan getPlan() {
        return plan;
    }

    public void setPlan(ResearchPlan plan) {
        this.plan = plan;
    }

    public String getRawModelJson() {
        return rawModelJson;
    }

    public void setRawModelJson(String rawModelJson) {
        this.rawModelJson = rawModelJson;
    }
}

