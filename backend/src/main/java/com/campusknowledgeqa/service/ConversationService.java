package com.campusknowledgeqa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusknowledgeqa.dto.ChatMessage;
import com.campusknowledgeqa.entity.QaConversation;
import com.campusknowledgeqa.entity.QaMessage;
import com.campusknowledgeqa.entity.QaRecord;
import com.campusknowledgeqa.mapper.QaConversationMapper;
import com.campusknowledgeqa.mapper.QaMessageMapper;
import com.campusknowledgeqa.mapper.QaRecordMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** 会话 / 消息 / 问答记录 */
@Service
public class ConversationService {

    private final QaConversationMapper conversationMapper;
    private final QaMessageMapper messageMapper;
    private final QaRecordMapper recordMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ConversationService(QaConversationMapper conversationMapper, QaMessageMapper messageMapper,
                               QaRecordMapper recordMapper) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.recordMapper = recordMapper;
    }

    /** 新建会话，标题缺省取问题前 30 字 */
    public QaConversation create(Long userId, String title) {
        QaConversation conversation = new QaConversation();
        conversation.setUserId(userId);
        conversation.setTitle(trimTitle(title));
        conversationMapper.insert(conversation);
        return conversationMapper.selectById(conversation.getId());
    }

    public List<QaMessage> messages(Long conversationId) {
        return messageMapper.selectList(new LambdaQueryWrapper<QaMessage>()
                .eq(QaMessage::getConversationId, conversationId)
                .orderByAsc(QaMessage::getId));
    }

    /** 会话历史（供多轮对话使用） */
    public List<ChatMessage> history(Long conversationId) {
        List<ChatMessage> history = new ArrayList<>();
        for (QaMessage message : messages(conversationId)) {
            history.add(new ChatMessage(message.getRole(), message.getContent()));
        }
        return history;
    }

    public void addMessage(Long conversationId, String role, String content, List<String> sources) {
        QaMessage message = new QaMessage();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        message.setSources(toJson(sources));
        messageMapper.insert(message);
    }

    public Long addRecord(Long userId, Long conversationId, String question, String answer,
                          List<String> sources) {
        QaRecord record = new QaRecord();
        record.setUserId(userId);
        record.setConversationId(conversationId);
        record.setQuestion(question);
        record.setAnswer(answer);
        record.setSources(toJson(sources));
        record.setFeedback("NONE");
        record.setIsFavorite(0);
        record.setSensitiveHit(0);
        recordMapper.insert(record);
        return record.getId();
    }

    /** 同步保存一轮对话：assistant 消息 + 问答记录，返回问答记录ID */
    public Long saveTurn(Long conversationId, Long userId, String question, String answer,
                         List<String> sources) {
        addMessage(conversationId, "assistant", answer, sources);
        return addRecord(userId, conversationId, question, answer, sources);
    }

    /** 保存被敏感词拦截的一轮：落 assistant 消息 + sensitive_hit=1 的记录 */
    public Long saveBlockedTurn(Long conversationId, Long userId, String question, String answer) {
        addMessage(conversationId, "assistant", answer, List.of());
        QaRecord record = new QaRecord();
        record.setUserId(userId);
        record.setConversationId(conversationId);
        record.setQuestion(question);
        record.setAnswer(answer);
        record.setSources("[]");
        record.setFeedback("NONE");
        record.setIsFavorite(0);
        record.setSensitiveHit(1);
        recordMapper.insert(record);
        return record.getId();
    }

    /** 异步保存一轮对话：assistant 消息 + 问答记录 */
    @Async
    public void saveTurnAsync(Long conversationId, Long userId, String question, String answer,
                              List<String> sources) {
        saveTurn(conversationId, userId, question, answer, sources);
    }

    private String trimTitle(String title) {
        if (title == null || title.isBlank()) {
            return "新会话";
        }
        String trimmed = title.trim();
        return trimmed.length() > 30 ? trimmed.substring(0, 30) : trimmed;
    }

    private String toJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list == null ? List.of() : list);
        } catch (Exception e) {
            return "[]";
        }
    }
}
