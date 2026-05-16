package com.quant.platform.ai.core.langchain4j.research.plan;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.stereotype.Service;

/**
 * 生成结构化计划（JSON），用于 Plan-Act。
 */
@Service
public class ResearchPlanner {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ChatModel model;

    public ResearchPlanner(ChatModel model) {
        this.model = model;
    }

    public PlanResult plan(String symbol, String message) {
        String toolList = "- fundamentalEvaluate(symbol)\n"
                + "- technicalEvaluate(symbol, minuteLimit)\n"
                + "- sentimentEvaluate(symbol)\n";

        String prompt = "你是量化投研 Agent 的规划器(Planner)。\n"
                + "请基于用户问题产出一个 JSON 计划，用于后续逐步调用工具。\n"
                + "要求：\n"
                + "1) 只能使用以下工具名：fundamentalEvaluate / technicalEvaluate / sentimentEvaluate\n"
                + "2) args 必须是 JSON 对象，字段与工具参数名一致\n"
                + "3) steps 最多 6 步\n"
                + "4) 只输出 JSON，不要输出任何解释文本\n"
                + "\n"
                + "可用工具：\n"
                + toolList
                + "\n"
                + "默认：若用户未指定 symbol，但你需要，请在 args.symbol 使用传入的 symbol（若 symbol 为空则不要强行调用需要 symbol 的工具）。\n"
                + "\n"
                + "输入：\n"
                + "symbol=" + (symbol == null ? "" : symbol) + "\n"
                + "message=" + message + "\n"
                + "\n"
                + "输出 JSON Schema 示例：\n"
                + "{\n"
                + "  \"objective\": \"...\",\n"
                + "  \"steps\": [\n"
                + "    {\"tool\": \"fundamentalEvaluate\", \"args\": {\"symbol\": \"000001.SZ\"}, \"expected\": \"...\"}\n"
                + "  ]\n"
                + "}\n";

        String json = model.chat(prompt);
        try {
            ResearchPlan plan = MAPPER.readValue(json, ResearchPlan.class);
            return PlanResult.ok(prompt, json, plan);
        } catch (Exception e) {
            return PlanResult.fail(prompt, json, e.toString());
        }
    }

    public static final class PlanResult {
        private final boolean ok;
        private final String plannerPrompt;
        private final String rawModelJson;
        private final ResearchPlan plan;
        private final String error;

        private PlanResult(boolean ok, String plannerPrompt, String rawModelJson, ResearchPlan plan, String error) {
            this.ok = ok;
            this.plannerPrompt = plannerPrompt;
            this.rawModelJson = rawModelJson;
            this.plan = plan;
            this.error = error;
        }

        static PlanResult ok(String plannerPrompt, String rawModelJson, ResearchPlan plan) {
            return new PlanResult(true, plannerPrompt, rawModelJson, plan, null);
        }

        static PlanResult fail(String plannerPrompt, String rawModelJson, String error) {
            return new PlanResult(false, plannerPrompt, rawModelJson, null, error);
        }

        public boolean isOk() {
            return ok;
        }

        public String getPlannerPrompt() {
            return plannerPrompt;
        }

        public String getRawModelJson() {
            return rawModelJson;
        }

        public ResearchPlan getPlan() {
            return plan;
        }

        public String getError() {
            return error;
        }
    }
}

