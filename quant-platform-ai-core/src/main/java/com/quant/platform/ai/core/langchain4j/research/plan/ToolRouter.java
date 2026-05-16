package com.quant.platform.ai.core.langchain4j.research.plan;

import com.quant.platform.ai.core.langchain4j.QuantAiTools;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工具路由 + 最小权限隔离（研究模式仅允许只读工具）。
 */
@Service
public class ToolRouter {
    private final QuantAiTools tools;

    public ToolRouter(QuantAiTools tools) {
        this.tools = tools;
    }

    public Map<String, Object> call(String tool, Map<String, Object> args) {
        if (tool == null) {
            return fail("tool 为空");
        }
        String t = tool.trim();
        Map<String, Object> a = args == null ? Map.of() : args;

        // allow-list（研究模式）
        if ("fundamentalEvaluate".equals(t)) {
            String symbol = str(a.get("symbol"));
            return tools.fundamentalEvaluate(symbol);
        }
        if ("technicalEvaluate".equals(t)) {
            String symbol = str(a.get("symbol"));
            int minuteLimit = intv(a.get("minuteLimit"), 2000);
            // hard cap，避免一次拉太多分钟线
            minuteLimit = Math.min(Math.max(minuteLimit, 0), 10000);
            return tools.technicalEvaluate(symbol, minuteLimit);
        }
        if ("sentimentEvaluate".equals(t)) {
            String symbol = str(a.get("symbol"));
            return tools.sentimentEvaluate(symbol);
        }
        return fail("工具未允许或不存在: " + t);
    }

    private static Map<String, Object> fail(String msg) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", false);
        out.put("error", msg);
        return out;
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static int intv(Object v, int dft) {
        if (v == null) {
            return dft;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (Exception ignored) {
            return dft;
        }
    }
}

