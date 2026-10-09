package com.campusknowledgeqa.service;

import com.campusknowledgeqa.dto.ChatMessage;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 查询改写（指代消解）：结合对话历史，把用户追问改写成独立、适合知识库检索的问题。
 * 例：
 *   历史：图书馆几点开门？
 *   追问：那它周末开吗？
 *   改写：图书馆周末开放吗？
 */
@Service
public class QueryRewriter {

    private static final String ROLE_USER = "user";

    private final ChatClient chatClient;

    public QueryRewriter(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String rewrite(String question, List<ChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return question;
        }
        String prompt = """
                下面是一段用户与助手的对话历史，以及用户最新的追问。
                请结合历史，把最新追问改写成一个不依赖上下文、语义完整、适合用于知识库检索的独立问题。
                要求：只输出改写后的问题本身，不要解释、不要加引号、不要换行。

                对话历史：
                %s
                最新追问：%s
                """.formatted(buildHistoryText(history), question);
        try {
            String rewritten = chatClient.prompt().user(prompt).call().content();
            if (rewritten != null && !rewritten.isBlank()) {
                String result = rewritten.trim().replace("\n", "");
                System.out.println("🔎 查询改写：" + question + "  ->  " + result);
                return result;
            }
        } catch (Exception e) {
            System.out.println("⚠️ 查询改写失败，回退为拼接检索：" + e.getMessage());
        }
        return fallback(question, history);
    }

    private String buildHistoryText(List<ChatMessage> history) {
        StringBuilder sb = new StringBuilder();
        int from = Math.max(0, history.size() - 6); // 最近 3 轮
        for (int i = from; i < history.size(); i++) {
            ChatMessage message = history.get(i);
            if (message == null || message.content() == null || message.content().isBlank()) {
                continue;
            }
            sb.append(ROLE_USER.equals(message.role()) ? "用户：" : "助手：")
                    .append(message.content()).append("\n");
        }
        return sb.toString();
    }

    /** 退化方案：改写失败时，把最近一条用户问题与当前问题拼接后检索 */
    private String fallback(String question, List<ChatMessage> history) {
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessage message = history.get(i);
            if (message != null && ROLE_USER.equals(message.role())
                    && message.content() != null && !message.content().isBlank()) {
                return message.content() + " " + question;
            }
        }
        return question;
    }
}
