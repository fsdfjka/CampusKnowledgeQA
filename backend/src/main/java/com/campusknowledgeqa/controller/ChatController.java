package com.campusknowledgeqa.controller;

import com.campusknowledgeqa.common.Result;
import com.campusknowledgeqa.dto.ChatMessage;
import com.campusknowledgeqa.dto.ChatRequest;
import com.campusknowledgeqa.service.ConversationService;
import com.campusknowledgeqa.service.HybridRetriever;
import com.campusknowledgeqa.service.QueryRewriter;
import com.campusknowledgeqa.service.SensitiveWordService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

@RestController
public class ChatController {

    private static final int TOP_K = 3;
    /** 多轮对话最多携带的历史轮数（1 轮 = user + assistant） */
    private static final int MAX_HISTORY_TURNS = 5;

    /** SSE 末尾回传问答记录ID的标记前缀（\u0001 为控制字符，避免与正文冲突） */
    private static final String RECORD_ID_PREFIX = "\u0001RECORD_ID:";

    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";

    private final ChatClient chatClient;
    private final HybridRetriever hybridRetriever;
    private final QueryRewriter queryRewriter;
    private final ConversationService conversationService;
    private final SensitiveWordService sensitiveWordService;

    public ChatController(ChatClient chatClient, HybridRetriever hybridRetriever,
                          QueryRewriter queryRewriter, ConversationService conversationService,
                          SensitiveWordService sensitiveWordService) {
        this.chatClient = chatClient;
        this.hybridRetriever = hybridRetriever;
        this.queryRewriter = queryRewriter;
        this.conversationService = conversationService;
        this.sensitiveWordService = sensitiveWordService;
    }

    /** 单轮问答（兼容旧接口） */
    @GetMapping("/chat")
    public Result<String> chat(@RequestParam String question) {
        if (question == null || question.isBlank()) {
            return Result.fail(4001, "问题不能为空");
        }
        String hit = sensitiveWordService.findHit(question);
        if (hit != null) {
            return Result.success(blockMessage(hit));
        }
        ContextResult ctx = buildContext(question);
        String answer = chatClient.prompt()
                .messages(buildMessages(question, List.of(), ctx.context()))
                .call()
                .content();
        return Result.success(answer);
    }

    /**
     * 多轮流式问答（SSE）：携带 conversationId，服务端取历史并落库
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequest request, Authentication authentication) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            return Flux.just("问题不能为空");
        }
        Long userId = currentUserId(authentication);
        Long conversationId = request.conversationId();

        // 敏感词拦截：命中则不调用大模型
        String sensitiveHit = sensitiveWordService.findHit(request.question());
        if (sensitiveHit != null) {
            String block = blockMessage(sensitiveHit);
            if (conversationId != null) {
                conversationService.saveBlockedTurn(conversationId, userId, request.question(), block);
            }
            return Flux.just(block);
        }

        // 历史：优先服务端会话（方案B）；无会话时用请求里的 history
        List<ChatMessage> history = (conversationId != null)
                ? conversationService.history(conversationId)
                : (request.history() == null ? List.of() : request.history());

        // 查询改写（指代消解）
        String retrievalQuery = queryRewriter.rewrite(request.question(), history);
        ContextResult contextResult = buildContext(retrievalQuery);

        // 落用户消息
        if (conversationId != null) {
            conversationService.addMessage(conversationId, ROLE_USER, request.question(), null);
        }

        StringBuilder answerBuffer = new StringBuilder();
        Flux<String> answerFlux = chatClient.prompt()
                .messages(buildMessages(request.question(), history, contextResult.context()))
                .stream()
                .content()
                .doOnNext(answerBuffer::append);

        // 流结束后同步落库（assistant 消息 + 问答记录），并把 recordId 作为末尾事件回传
        Mono<String> recordIdMono = Mono.fromCallable(() -> {
            if (conversationId == null) {
                return "";
            }
            Long recordId = conversationService.saveTurn(conversationId, userId, request.question(),
                    answerBuffer.toString(), contextResult.sources());
            return recordId == null ? "" : RECORD_ID_PREFIX + recordId;
        }).subscribeOn(Schedulers.boundedElastic());

        return Flux.concat(answerFlux, recordIdMono)
                .filter(chunk -> chunk != null && !chunk.isEmpty())
                .onErrorResume(e -> Flux.just("\n【回答出错】" + e.getMessage()));
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object details = authentication.getDetails();
        return details instanceof Number number ? number.longValue() : null;
    }

    private String blockMessage(String word) {
        return "抱歉，你的问题包含违规内容「" + word + "」，已被系统拦截，请重新提问。";
    }

    /**
     * 组装消息列表：System(含 RAG 上下文) + 历史多轮消息（截断） + 当前问题
     */
    private List<Message> buildMessages(String question, List<ChatMessage> history, String context) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt(context)));

        for (ChatMessage message : trimHistory(history)) {
            if (message.content() == null || message.content().isBlank()) {
                continue;
            }
            if (ROLE_ASSISTANT.equals(message.role())) {
                messages.add(new AssistantMessage(message.content()));
            } else if (ROLE_USER.equals(message.role())) {
                messages.add(new UserMessage(message.content()));
            }
        }

        messages.add(new UserMessage(question));
        return messages;
    }

    private List<ChatMessage> trimHistory(List<ChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        int maxMessages = MAX_HISTORY_TURNS * 2;
        return history.size() > maxMessages
                ? history.subList(history.size() - maxMessages, history.size())
                : history;
    }

    private String systemPrompt(String context) {
        return """
                你是校园知识库问答助手，请严格根据下面【知识库内容】回答用户问题。
                如果知识库没有相关信息，直接回答：暂无相关校园信息，不要编造内容。
                结合上下文理解用户的多轮提问。
                【知识库内容】：
                """ + context;
    }

    /**
     * 混合检索：向量检索 + 关键词检索，RRF 融合，拼成带来源的上下文
     */
    private ContextResult buildContext(String retrievalQuery) {
        List<Document> hits = hybridRetriever.retrieve(retrievalQuery, TOP_K);
        if (hits.isEmpty()) {
            return new ContextResult("（无相关知识库内容）", List.of());
        }
        StringBuilder context = new StringBuilder();
        List<String> sources = new ArrayList<>();
        for (Document doc : hits) {
            String source = String.valueOf(doc.getMetadata().getOrDefault("source", "未知来源"));
            if (!sources.contains(source)) {
                sources.add(source);
            }
            context.append("【来源：").append(source).append("】\n")
                    .append(doc.getText()).append("\n\n");
        }
        return new ContextResult(context.toString(), sources);
    }

    private record ContextResult(String context, List<String> sources) {
    }
}
