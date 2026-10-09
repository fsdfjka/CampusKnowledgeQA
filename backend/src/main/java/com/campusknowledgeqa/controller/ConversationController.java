package com.campusknowledgeqa.controller;

import com.campusknowledgeqa.common.Result;
import com.campusknowledgeqa.entity.QaConversation;
import com.campusknowledgeqa.entity.QaMessage;
import com.campusknowledgeqa.service.ConversationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    /** 新建会话，返回 conversationId */
    @PostMapping("/conversations")
    public Result<Map<String, Object>> create(@RequestBody(required = false) Map<String, String> body,
                                              Authentication authentication) {
        Long userId = currentUserId(authentication);
        String title = body == null ? null : body.get("title");
        QaConversation conversation = conversationService.create(userId, title);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", conversation.getId());
        data.put("title", conversation.getTitle());
        return Result.success(data);
    }

    /** 会话消息列表 */
    @GetMapping("/conversations/{id}/messages")
    public Result<List<QaMessage>> messages(@PathVariable Long id) {
        return Result.success(conversationService.messages(id));
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object details = authentication.getDetails();
        return details instanceof Number number ? number.longValue() : null;
    }
}
