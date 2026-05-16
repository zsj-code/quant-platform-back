package com.quant.platform.business.agent;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 跟踪进行中的 Research Agent 任务，支持按 sessionId / runId 停止。
 */
@Component
@ConditionalOnProperty(prefix = "quant.ai.langchain4j.openai", name = "api-key")
public class ResearchAgentRunManager {

    private static final MediaType TEXT_PLAIN_UTF8 =
        new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8);

    private final Executor researchExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "research-agent");
        t.setDaemon(true);
        return t;
    });

    private final AgentRunAuditService audit;
    private final Map<String, ActiveRun> bySessionId = new ConcurrentHashMap<>();
    private final Map<String, ActiveRun> byRunId = new ConcurrentHashMap<>();

    public ResearchAgentRunManager(AgentRunAuditService audit) {
        this.audit = audit;
    }

    public Executor researchExecutor() {
        return researchExecutor;
    }

    public ActiveRun register(String sessionId) {
        String sid = requireSessionId(sessionId);
        ActiveRun run = new ActiveRun(sid);
        bySessionId.put(sid, run);
        return run;
    }

    public void bindRunMeta(ActiveRun run, String runId, String workflowRunKey) {
        if (run == null || runId == null || runId.isBlank()) {
            return;
        }
        run.runId = runId.trim();
        run.workflowRunKey = workflowRunKey;
        byRunId.put(run.runId, run);
    }

    public void remove(ActiveRun run) {
        if (run == null) {
            return;
        }
        bySessionId.remove(run.sessionId, run);
        if (run.runId != null) {
            byRunId.remove(run.runId, run);
        }
    }

    /**
     * @return 是否找到并触发了停止
     */
    public boolean stop(String sessionId, String runId, String reason) {
        ActiveRun run = resolve(sessionId, runId);
        if (run == null) {
            return false;
        }
        run.cancel(reason == null || reason.isBlank() ? "用户停止" : reason.trim(), audit);
        return true;
    }

    private ActiveRun resolve(String sessionId, String runId) {
        if (sessionId != null && !sessionId.isBlank()) {
            ActiveRun run = bySessionId.get(sessionId.trim());
            if (run != null) {
                return run;
            }
        }
        if (runId != null && !runId.isBlank()) {
            return byRunId.get(runId.trim());
        }
        return null;
    }

    private static String requireSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId 不能为空");
        }
        return sessionId.trim();
    }

    public static final class ActiveRun {
        private final String sessionId;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        private volatile String runId;
        private volatile String workflowRunKey;
        private volatile SseEmitter sseEmitter;
        private volatile CompletableFuture<?> task;
        private volatile Thread workerThread;

        private ActiveRun(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getRunId() {
            return runId;
        }

        public void attachSse(SseEmitter emitter) {
            this.sseEmitter = emitter;
        }

        public void attachTask(CompletableFuture<?> future) {
            this.task = future;
        }

        public void bindWorkerThread() {
            this.workerThread = Thread.currentThread();
        }

        public boolean isCancelled() {
            return cancelled.get() || Thread.currentThread().isInterrupted();
        }

        public void checkCancelled() {
            if (isCancelled()) {
                throw new ResearchAgentCancelledException("用户已停止当前任务");
            }
        }

        void cancel(String reason, AgentRunAuditService audit) {
            if (!cancelled.compareAndSet(false, true)) {
                return;
            }
            CompletableFuture<?> f = task;
            if (f != null) {
                f.cancel(true);
            }
            Thread t = workerThread;
            if (t != null) {
                t.interrupt();
            }
            if (workflowRunKey != null && audit != null) {
                audit.markCancelled(workflowRunKey, reason);
            }
            completeSseCancelled();
        }

        private void completeSseCancelled() {
            SseEmitter emitter = sseEmitter;
            if (emitter == null) {
                return;
            }
            try {
                emitter.send(SseEmitter.event().name("stage").data("cancelled", TEXT_PLAIN_UTF8));
                emitter.send(SseEmitter.event().name("done").data("", TEXT_PLAIN_UTF8));
                emitter.complete();
            } catch (Exception ignored) {
                // ignored
            }
        }
    }
}
