package com.heizi.ai.dto;

import java.util.Map;

public record ChatRequest(
        String conversationId,
        String message,
        ChatMode mode,
        String knowledgeBaseId,
        String agentId,
        Map<String, Object> metadata
) {
}
