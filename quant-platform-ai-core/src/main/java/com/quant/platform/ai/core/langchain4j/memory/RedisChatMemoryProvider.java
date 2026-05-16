package com.quant.platform.ai.core.langchain4j.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 将 LangChain4j ChatMemory 持久化到 Redis（按 sessionId 隔离）。
 * <p>
 * 只存 user/ai 文本消息（工具调用中间态不落库），用于多轮对话上下文恢复。
 */
public class RedisChatMemoryProvider implements ChatMemoryProvider {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<StoredMessage>> LIST_TYPE = new TypeReference<List<StoredMessage>>() {
    };

    private final StringRedisTemplate redis;
    private final int maxMessages;
    private final Duration ttl;

    public RedisChatMemoryProvider(StringRedisTemplate redis, int maxMessages, Duration ttl) {
        this.redis = redis;
        this.maxMessages = maxMessages;
        this.ttl = ttl;
    }

    @Override
    public ChatMemory get(Object memoryId) {
        String sessionId = memoryId == null ? "default" : String.valueOf(memoryId);
        String key = key(sessionId);

        MessageWindowChatMemory mem = MessageWindowChatMemory.withMaxMessages(maxMessages);
        String json = redis.opsForValue().get(key);
        if (json != null && !json.isBlank()) {
            try {
                List<StoredMessage> stored = MAPPER.readValue(json, LIST_TYPE);
                for (StoredMessage sm : stored) {
                    ChatMessage msg = sm.toChatMessage();
                    if (msg != null) {
                        mem.add(msg);
                    }
                }
            } catch (Exception ignored) {
                // 读不到就从空记忆开始
            }
        }

        // 包一层：在 add() 后自动回写 Redis
        return new PersistingChatMemory(mem, redis, key, ttl);
    }

    private static String key(String sessionId) {
        return "agent:session:" + sessionId;
    }

    private static final class PersistingChatMemory implements ChatMemory {
        private final MessageWindowChatMemory delegate;
        private final StringRedisTemplate redis;
        private final String key;
        private final Duration ttl;

        private PersistingChatMemory(MessageWindowChatMemory delegate,
                                     StringRedisTemplate redis,
                                     String key,
                                     Duration ttl) {
            this.delegate = delegate;
            this.redis = redis;
            this.key = key;
            this.ttl = ttl;
        }

        @Override
        public Object id() {
            return delegate.id();
        }

        @Override
        public List<ChatMessage> messages() {
            return delegate.messages();
        }

        @Override
        public void add(ChatMessage message) {
            delegate.add(message);
            persist();
        }

        @Override
        public void clear() {
            delegate.clear();
            redis.delete(key);
        }

        private void persist() {
            List<StoredMessage> out = new ArrayList<>();
            for (ChatMessage m : delegate.messages()) {
                StoredMessage sm = StoredMessage.from(m);
                if (sm != null) {
                    out.add(sm);
                }
            }
            try {
                String json = MAPPER.writeValueAsString(out);
                redis.opsForValue().set(key, json, ttl);
            } catch (Exception ignored) {
            }
        }
    }

    private static final class StoredMessage {
        private String role;
        private String text;

        public StoredMessage() {
        }

        static StoredMessage from(ChatMessage m) {
            if (m instanceof UserMessage) {
                StoredMessage sm = new StoredMessage();
                sm.role = "user";
                UserMessage um = (UserMessage) m;
                sm.text = um.hasSingleText() ? um.singleText() : String.valueOf(m);
                return sm;
            }
            if (m instanceof AiMessage) {
                StoredMessage sm = new StoredMessage();
                sm.role = "ai";
                sm.text = ((AiMessage) m).text();
                return sm;
            }
            return null;
        }

        ChatMessage toChatMessage() {
            if ("user".equalsIgnoreCase(role)) {
                return UserMessage.from(text == null ? "" : text);
            }
            if ("ai".equalsIgnoreCase(role)) {
                return AiMessage.from(text == null ? "" : text);
            }
            return null;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}

