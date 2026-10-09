package com.campusknowledgeqa.dto;

import java.util.List;

/** 多轮问答请求：会话ID + 当前问题 + 历史消息（conversationId 优先，历史走服务端） */
public record ChatRequest(Long conversationId, String question, List<ChatMessage> history) {
}
